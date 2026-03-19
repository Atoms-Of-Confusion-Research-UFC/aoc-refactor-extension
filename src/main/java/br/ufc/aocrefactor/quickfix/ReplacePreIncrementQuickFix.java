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

        PsiPrefixExpression prefixExpr = (PsiPrefixExpression) descriptor.getPsiElement();

        IElementType op = prefixExpr.getOperationTokenType();
        PsiExpression operand = prefixExpr.getOperand();
        if (operand == null) return;

        String varName = operand.getText();                          // ex: "a"
        String opSymbol = op == JavaTokenType.PLUSPLUS ? "++" : "--"; // "++" ou "--"

        // Sobe até o statement pai (linha inteira)
        PsiStatement parentStatement =
                PsiTreeUtil.getParentOfType(prefixExpr, PsiStatement.class);
        if (parentStatement == null) return;

        PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);

        // Cria o statement separado: "a++;" ou "a--;"
        // Usamos pós-incremento aqui pois o efeito é o mesmo quando isolado
        PsiStatement incrementStatement = factory.createStatementFromText(
                varName + opSymbol + ";", null
        );

        // Substitui ++a / --a pelo nome simples da variável na expressão original
        PsiExpression simpleVar = factory.createExpressionFromText(varName, null);
        prefixExpr.replace(simpleVar);

        // Insere o incremento/decremento ANTES do statement pai
        PsiElement parent = parentStatement.getParent();
        parent.addBefore(incrementStatement, parentStatement);
    }
}