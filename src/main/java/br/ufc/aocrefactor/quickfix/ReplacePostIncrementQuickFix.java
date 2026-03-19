package br.ufc.aocrefactor.quickfix;

import com.intellij.codeInspection.*;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

public class ReplacePostIncrementQuickFix implements LocalQuickFix {

    @Override
    public @NotNull String getName() {
        return "Separate post-increment/decrement into its own line";
    }

    @Override
    public @NotNull String getFamilyName() {
        return getName();
    }

    @Override
    public void applyFix(@NotNull Project project,
                         @NotNull ProblemDescriptor descriptor) {

        // O elemento agora pode ser o pai — busca o PsiPostfixExpression dentro dele
        PsiElement element = descriptor.getPsiElement();
        PsiPostfixExpression postfixExpr = findPostfixExpression(element);
        if (postfixExpr == null) return;

        IElementType op = postfixExpr.getOperationTokenType();
        PsiExpression operand = postfixExpr.getOperand();
        if (operand == null) return;

        String varName = operand.getText();
        String opSymbol = op == JavaTokenType.PLUSPLUS ? "++" : "--";

        PsiStatement parentStatement =
                PsiTreeUtil.getParentOfType(postfixExpr, PsiStatement.class);
        if (parentStatement == null) return;

        PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);

        PsiStatement incrementStatement = factory.createStatementFromText(
                varName + opSymbol + ";", null
        );

        PsiExpression simpleVar = factory.createExpressionFromText(varName, null);
        postfixExpr.replace(simpleVar);

        // Incremento vai DEPOIS do statement pai
        PsiElement parent = parentStatement.getParent();
        parent.addAfter(incrementStatement, parentStatement);
    }

    // Busca o PsiPostfixExpression: pode ser o próprio elemento ou um filho
    private PsiPostfixExpression findPostfixExpression(PsiElement element) {
        if (element instanceof PsiPostfixExpression postfix) {
            return postfix;
        }
        return PsiTreeUtil.findChildOfType(element, PsiPostfixExpression.class);
    }
}