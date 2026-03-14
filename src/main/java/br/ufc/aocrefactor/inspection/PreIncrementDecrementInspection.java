package br.ufc.aocrefactor.inspection;

import br.ufc.aocrefactor.quickfix.ReplacePreIncrementQuickFix;
import com.intellij.codeInspection.*;
import com.intellij.psi.*;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

public class PreIncrementDecrementInspection extends AbstractBaseJavaLocalInspectionTool {

    @Override
    public @NotNull PsiElementVisitor buildVisitor(
            @NotNull ProblemsHolder holder,
            boolean isOnTheFly) {

        return new JavaElementVisitor() {
            @Override
            public void visitPrefixExpression(@NotNull PsiPrefixExpression expression) {
                super.visitPrefixExpression(expression);

                // Verifica se é ++ ou -- (prefix)
                IElementType op = expression.getOperationTokenType();
                if (!isPreIncrementOrDecrement(op)) return;

                // Verifica o pai — só marca se estiver nos contextos do algoritmo
                PsiElement parent = expression.getParent();

                if (isValidContext(parent)) {
                    String opName = op == JavaTokenType.PLUSPLUS ? "pré-incremento (++)" : "pré-decremento (--)";
                    holder.registerProblem(
                            expression,
                            "Átomo de confusão: operador de " + opName + " pode causar confusão. Considere separar a operação.",
                            ProblemHighlightType.WARNING,
                            new ReplacePreIncrementQuickFix()
                    );
                }
            }
        };
    }

    // Apenas ++ e -- prefixados (não pós-fixados)
    private boolean isPreIncrementOrDecrement(IElementType op) {
        return op == JavaTokenType.PLUSPLUS || op == JavaTokenType.MINUSMINUS;
    }

    // Contextos do algoritmo:
    // 1. Atribuição de variável       → PsiAssignmentExpression ou PsiLocalVariable
    // 2. Operação binária             → PsiBinaryExpression / PsiPolyadicExpression
    // 3. Parâmetro de método          → PsiExpressionList (filho de PsiMethodCallExpression)
    // 4. Índice de array              → PsiArrayAccessExpression
    // 5. Return de método             → PsiReturnStatement
    // 6. Condição de if/while/for     → PsiBinaryExpression já cobre, mas PsiIfStatement também
    private boolean isValidContext(PsiElement parent) {
        return parent instanceof PsiAssignmentExpression
                || parent instanceof PsiLocalVariable
                || parent instanceof PsiBinaryExpression
                || parent instanceof PsiPolyadicExpression
                || parent instanceof PsiExpressionList       // parâmetro de método
                || parent instanceof PsiArrayAccessExpression
                || parent instanceof PsiReturnStatement;
    }
}