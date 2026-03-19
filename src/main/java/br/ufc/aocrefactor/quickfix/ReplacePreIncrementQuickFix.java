package br.ufc.aocrefactor.quickfix;

import com.intellij.codeInspection.*;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

public class ReplacePreIncrementQuickFix implements LocalQuickFix {

    @Override
    public @NotNull String getName() {
        return "Separate pre-increment/decrement into its own line";
    }

    @Override
    public @NotNull String getFamilyName() {
        return getName();
    }

    @Override
    public void applyFix(@NotNull Project project,
                         @NotNull ProblemDescriptor descriptor) {

        // O elemento agora pode ser o pai — busca o PsiPrefixExpression dentro dele
        PsiElement element = descriptor.getPsiElement();
        PsiPrefixExpression prefixExpr = findPrefixExpression(element);
        if (prefixExpr == null) return;

        IElementType op = prefixExpr.getOperationTokenType();
        PsiExpression operand = prefixExpr.getOperand();
        if (operand == null) return;

        String varName = operand.getText();
        String opSymbol = op == JavaTokenType.PLUSPLUS ? "++" : "--";

        PsiStatement parentStatement =
                PsiTreeUtil.getParentOfType(prefixExpr, PsiStatement.class);
        if (parentStatement == null) return;

        PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);

        PsiStatement incrementStatement = factory.createStatementFromText(
                varName + opSymbol + ";", null
        );

        PsiExpression simpleVar = factory.createExpressionFromText(varName, null);
        prefixExpr.replace(simpleVar);

        // Incremento vai ANTES do statement pai
        PsiElement parent = parentStatement.getParent();
        parent.addBefore(incrementStatement, parentStatement);
    }

    // Busca o PsiPrefixExpression: pode ser o próprio elemento ou um filho
    private PsiPrefixExpression findPrefixExpression(PsiElement element) {
        if (element instanceof PsiPrefixExpression prefix) {
            return prefix;
        }
        return PsiTreeUtil.findChildOfType(element, PsiPrefixExpression.class);
    }
}