package br.ufc.aocrefactor.inspection;

import br.ufc.aocrefactor.quickfix.ReplacePostIncrementQuickFix;
import com.intellij.codeInspection.*;
import com.intellij.psi.*;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

public class PostIncrementDecrementInspection extends AbstractBaseJavaLocalInspectionTool {

    @Override
    public @NotNull PsiElementVisitor buildVisitor(
            @NotNull ProblemsHolder holder,
            boolean isOnTheFly) {

        return new JavaElementVisitor() {
            @Override
            public void visitPostfixExpression(@NotNull PsiPostfixExpression expression) {
                super.visitPostfixExpression(expression);

                // Verifica se é ++ ou -- (postfix)
                IElementType op = expression.getOperationTokenType();
                if (!isPostIncrementOrDecrement(op)) return;

                // Verifica o pai — só marca se estiver nos contextos do algoritmo
                PsiElement parent = expression.getParent();

                if (isValidContext(parent)) {
                    String opName = op == JavaTokenType.PLUSPLUS ? "pós-incremento (++)" : "pós-decremento (--)";
                    holder.registerProblem(
                            expression,
                            "Átomo de confusão: operador de " + opName + " pode causar confusão. Considere separar a operação.",
                            ProblemHighlightType.WARNING,
                            new ReplacePostIncrementQuickFix()
                    );
                }
            }
        };
    }

    private boolean isPostIncrementOrDecrement(IElementType op) {
        return op == JavaTokenType.PLUSPLUS || op == JavaTokenType.MINUSMINUS;
    }

    // Mesmos contextos do algoritmo:
    // 1. Atribuição de variável       → PsiAssignmentExpression ou PsiLocalVariable
    // 2. Operação binária             → PsiBinaryExpression / PsiPolyadicExpression
    // 3. Parâmetro de método          → PsiExpressionList
    // 4. Índice de array              → PsiArrayAccessExpression
    // 5. Return de método             → PsiReturnStatement
    private boolean isValidContext(PsiElement parent) {
        return parent instanceof PsiAssignmentExpression
                || parent instanceof PsiLocalVariable
                || parent instanceof PsiBinaryExpression
                || parent instanceof PsiPolyadicExpression
                || parent instanceof PsiExpressionList
                || parent instanceof PsiArrayAccessExpression
                || parent instanceof PsiReturnStatement;
    }
}