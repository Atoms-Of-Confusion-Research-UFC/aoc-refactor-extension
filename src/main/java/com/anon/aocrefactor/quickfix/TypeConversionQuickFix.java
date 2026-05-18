package com.anon.aocrefactor.quickfix;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

public class AddExplicitCommentQuickFix implements LocalQuickFix {

    @Override
    public @NotNull String getName() {
        return "Add comment flagging intentional narrowing conversion";
    }

    @Override
    public @NotNull String getFamilyName() {
        return getName();
    }

    @Override
    public void applyFix(@NotNull Project project,
                         @NotNull ProblemDescriptor descriptor) {

        PsiTypeCastExpression castExpr =
                (PsiTypeCastExpression) descriptor.getPsiElement();

        PsiType castType = castExpr.getCastType() != null
                ? castExpr.getCastType().getType() : null;
        PsiExpression operand = castExpr.getOperand();
        if (castType == null || operand == null) return;

        // Sobe até o statement pai
        PsiStatement parentStatement =
                PsiTreeUtil.getParentOfType(castExpr, PsiStatement.class);
        if (parentStatement == null) return;

        PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);

        // Cria o comentário: // Narrowing conversion: <fromType> → <toType>, possible precision loss
        String fromType = operand.getType() != null
                ? operand.getType().getPresentableText() : "?";
        String toType = castType.getPresentableText();

        String commentText = String.format(
                "// Narrowing conversion: %s -> %s (possible precision loss)",
                fromType, toType
        );

        PsiComment comment = factory.createCommentFromText(commentText, null);

        PsiElement parent = parentStatement.getParent();
        parent.addBefore(comment, parentStatement);
    }
}