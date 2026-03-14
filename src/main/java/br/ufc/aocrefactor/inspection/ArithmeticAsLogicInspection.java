package br.ufc.aocrefactor.inspection;

import br.ufc.aocrefactor.quickfix.ReplaceArithmeticAsLogicQuickFix;
import com.intellij.codeInspection.*;
import com.intellij.psi.*;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

public class ArithmeticAsLogicInspection extends AbstractBaseJavaLocalInspectionTool {

    @Override
    public @NotNull PsiElementVisitor buildVisitor(
            @NotNull ProblemsHolder holder,
            boolean isOnTheFly) {

        return new JavaElementVisitor() {
            @Override
            public void visitBinaryExpression(@NotNull PsiBinaryExpression expression) {
                super.visitBinaryExpression(expression);

                // Verifica se o operador é == ou !=
                IElementType op = expression.getOperationTokenType();
                if (!isEqualityOperator(op)) return;

                PsiExpression lhs = expression.getLOperand();
                PsiExpression rhs = expression.getROperand();
                if (rhs == null) return;

                // Verifica se um dos lados é zero
                boolean lhsIsZero = isZero(lhs);
                boolean rhsIsZero = isZero(rhs);
                if (!lhsIsZero && !rhsIsZero) return;

                // O lado que não é zero deve ser uma expressão aritmética
                PsiExpression arithmeticSide = rhsIsZero ? lhs : rhs;

                // Remove parênteses externos se houver
                arithmeticSide = unwrapParentheses(arithmeticSide);

                if (!isArithmeticAsLogicExpression(arithmeticSide)) return;

                holder.registerProblem(
                        expression,
                        "Átomo de confusão: aritmética usada como lógica. Considere usar operadores lógicos explícitos.",
                        ProblemHighlightType.WARNING,
                        new ReplaceArithmeticAsLogicQuickFix()
                );
            }
        };
    }

    // == ou !=
    private boolean isEqualityOperator(IElementType op) {
        return op == JavaTokenType.EQEQ || op == JavaTokenType.NE;
    }

    // Verifica se a expressão é literalmente "0"
    private boolean isZero(PsiExpression expr) {
        if (expr instanceof PsiLiteralExpression literal) {
            Object value = literal.getValue();
            if (value instanceof Number num) {
                return num.doubleValue() == 0;
            }
        }
        return false;
    }

    // Remove parênteses externos: (expr) → expr
    private PsiExpression unwrapParentheses(PsiExpression expr) {
        while (expr instanceof PsiParenthesizedExpression paren) {
            expr = paren.getExpression();
        }
        return expr;
    }

    // Verifica se a expressão é aritmética com *, +, ou -
    // Aceita também expressões compostas como (a - 3) * (7 - a)
    private boolean isArithmeticAsLogicExpression(PsiExpression expr) {
        if (expr instanceof PsiBinaryExpression bin) {
            IElementType op = bin.getOperationTokenType();
            return op == JavaTokenType.ASTERISK
                    || op == JavaTokenType.PLUS
                    || op == JavaTokenType.MINUS;
        }
        return false;
    }
}