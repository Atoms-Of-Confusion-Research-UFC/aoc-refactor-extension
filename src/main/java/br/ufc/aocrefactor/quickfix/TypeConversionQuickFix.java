package br.ufc.aocrefactor.quickfix;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

public class TypeConversionQuickFix implements LocalQuickFix {

    public enum Mode {
        ADD_COMMENT,
        CHANGE_TYPE
    }

    private final Mode mode;

    public TypeConversionQuickFix(Mode mode) {
        this.mode = mode;
    }

    @Override
    public @NotNull String getName() {
        return switch (mode) {
            case ADD_COMMENT -> "Add comment flagging intentional narrowing conversion";
            case CHANGE_TYPE -> "Change variable type to match source type (widen)";
        };
    }

    @Override
    public @NotNull String getFamilyName() {
        return "Fix narrowing type conversion";
    }

    @Override
    public void applyFix(@NotNull Project project,
                         @NotNull ProblemDescriptor descriptor) {
        switch (mode) {
            case ADD_COMMENT -> applyAddComment(project, descriptor);
            case CHANGE_TYPE -> applyChangeType(project, descriptor);
        }
    }

    // ---------------------------------------------------------------
    // Opção 1: adiciona comentário antes da linha
    // ---------------------------------------------------------------
    private void applyAddComment(@NotNull Project project,
                                 @NotNull ProblemDescriptor descriptor) {

        PsiTypeCastExpression castExpr =
                (PsiTypeCastExpression) descriptor.getPsiElement();

        PsiType castType = castExpr.getCastType() != null
                ? castExpr.getCastType().getType() : null;
        PsiExpression operand = castExpr.getOperand();
        if (castType == null || operand == null) return;

        PsiStatement parentStatement =
                PsiTreeUtil.getParentOfType(castExpr, PsiStatement.class);
        if (parentStatement == null) return;

        PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);

        String fromType = operand.getType() != null
                ? operand.getType().getPresentableText() : "?";
        String toType = castType.getPresentableText();

        PsiComment comment = factory.createCommentFromText(
                String.format("// Narrowing conversion: %s -> %s (possible precision loss)",
                        fromType, toType),
                null
        );

        PsiElement parent = parentStatement.getParent();
        parent.addBefore(comment, parentStatement);
    }

    // ---------------------------------------------------------------
    // Opção 2: altera o tipo da variável para o tipo maior (widening)
    // Remove o cast e ajusta o tipo declarado
    // Ex: byte b = (byte) a;  →  short b = a;
    // ---------------------------------------------------------------
    private void applyChangeType(@NotNull Project project,
                                 @NotNull ProblemDescriptor descriptor) {

        PsiTypeCastExpression castExpr =
                (PsiTypeCastExpression) descriptor.getPsiElement();

        PsiExpression operand = castExpr.getOperand();
        if (operand == null) return;

        PsiType sourceType = operand.getType();
        if (sourceType == null) return;

        PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);

        // Remove o cast — substitui (type) expr por expr
        PsiExpression withoutCast = factory.createExpressionFromText(
                operand.getText(), castExpr
        );
        PsiElement replacedExpr = castExpr.replace(withoutCast);

        // Sobe até a declaração de variável e altera o tipo
        PsiStatement parentStatement =
                PsiTreeUtil.getParentOfType(replacedExpr, PsiStatement.class);
        if (!(parentStatement instanceof PsiDeclarationStatement declStmt)) return;

        PsiLocalVariable var =
                (PsiLocalVariable) declStmt.getDeclaredElements()[0];

        // Substitui o tipo da variável pelo tipo fonte (maior)
        PsiTypeElement newTypeElement = factory.createTypeElement(sourceType);
        var.getTypeElement().replace(newTypeElement);
    }
}