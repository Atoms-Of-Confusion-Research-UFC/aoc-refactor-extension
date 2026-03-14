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
        return "Separar pós-incremento/decremento em linha própria";
    }

    @Override
    public @NotNull String getFamilyName() {
        return getName();
    }

    @Override
    public void applyFix(@NotNull Project project,
                         @NotNull ProblemDescriptor descriptor) {

        PsiPostfixExpression postfixExpr = (PsiPostfixExpression) descriptor.getPsiElement();

        IElementType op = postfixExpr.getOperationTokenType();
        PsiExpression operand = postfixExpr.getOperand();
        if (operand == null) return;

        String varName = operand.getText();                           // ex: "a"
        String opSymbol = op == JavaTokenType.PLUSPLUS ? "++" : "--"; // "++" ou "--"

        // Sobe até o statement pai (linha inteira)
        PsiStatement parentStatement =
                PsiTreeUtil.getParentOfType(postfixExpr, PsiStatement.class);
        if (parentStatement == null) return;

        PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);

        // Cria o statement separado: "a++;" ou "a--;"
        PsiStatement incrementStatement = factory.createStatementFromText(
                varName + opSymbol + ";", null
        );

        // Substitui a++/a-- pelo nome simples da variável na expressão original
        // O valor original é preservado pois o incremento vai DEPOIS
        PsiExpression simpleVar = factory.createExpressionFromText(varName, null);
        postfixExpr.replace(simpleVar);

        // Insere o incremento/decremento APÓS o statement pai
        // (diferença fundamental em relação ao pré-incremento)
        PsiElement parent = parentStatement.getParent();
        parent.addAfter(incrementStatement, parentStatement);
    }
}