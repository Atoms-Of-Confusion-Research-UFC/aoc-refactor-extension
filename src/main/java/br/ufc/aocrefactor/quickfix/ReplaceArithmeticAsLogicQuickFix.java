package br.ufc.aocrefactor.quickfix;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

public class ReplaceArithmeticAsLogicQuickFix implements LocalQuickFix {

    @Override
    public @NotNull String getName() {
        return "Substituir por expressão lógica equivalente";
    }

    @Override
    public @NotNull String getFamilyName() {
        return getName();
    }

    @Override
    public void applyFix(@NotNull Project project,
                         @NotNull ProblemDescriptor descriptor) {

        PsiBinaryExpression expression = (PsiBinaryExpression) descriptor.getPsiElement();

        IElementType equalityOp = expression.getOperationTokenType();
        boolean isEquals = equalityOp == JavaTokenType.EQEQ; // true = ==, false = !=

        PsiExpression lhs = expression.getLOperand();
        PsiExpression rhs = expression.getROperand();
        if (rhs == null) return;

        // Identifica qual lado é zero e qual é a expressão aritmética
        PsiExpression arithmeticSide = isZero(rhs) ? lhs : rhs;
        arithmeticSide = unwrapParentheses(arithmeticSide);

        if (!(arithmeticSide instanceof PsiBinaryExpression arithmetic)) return;

        IElementType arithmeticOp = arithmetic.getOperationTokenType();
        PsiExpression left  = arithmetic.getLOperand();
        PsiExpression right = arithmetic.getROperand();
        if (right == null) return;

        String leftText  = left.getText();
        String rightText = right.getText();

        PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);
        String replacement;

        // Multiplicação: a * b == 0 → a == 0 || b == 0
        //                a * b != 0 → a != 0 && b != 0
        if (arithmeticOp == JavaTokenType.ASTERISK) {
            String op      = isEquals ? "==" : "!=";
            String logical = isEquals ? "||" : "&&";
            replacement = String.format("%s %s 0 %s %s %s 0",
                    leftText, op, logical, rightText, op);

            // Subtração: a - b == 0 → a == b
            //            a - b != 0 → a != b
        } else if (arithmeticOp == JavaTokenType.MINUS) {
            String op = isEquals ? "==" : "!=";
            replacement = String.format("%s %s %s", leftText, op, rightText);

            // Adição: a + b == 0 → a == -b
            //         a + b != 0 → a != -b
        } else if (arithmeticOp == JavaTokenType.PLUS) {
            String op = isEquals ? "==" : "!=";
            replacement = String.format("%s %s -%s", leftText, op, rightText);

        } else {
            return;
        }

        PsiExpression newExpr = factory.createExpressionFromText(replacement, expression);
        expression.replace(newExpr);
    }

    private boolean isZero(PsiExpression expr) {
        if (expr instanceof PsiLiteralExpression literal) {
            Object value = literal.getValue();
            if (value instanceof Number num) {
                return num.doubleValue() == 0;
            }
        }
        return false;
    }

    private PsiExpression unwrapParentheses(PsiExpression expr) {
        while (expr instanceof PsiParenthesizedExpression paren) {
            expr = paren.getExpression();
        }
        return expr;
    }
}