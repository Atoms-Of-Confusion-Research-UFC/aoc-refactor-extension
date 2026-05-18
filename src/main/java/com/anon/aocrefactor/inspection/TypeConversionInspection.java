package com.anon.aocrefactor.inspection;

import com.anon.aocrefactor.quickfix.TypeConversionQuickFix;
import com.intellij.codeInspection.*;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

public class TypeConversionInspection extends AbstractBaseJavaLocalInspectionTool {

    @Override
    public @NotNull PsiElementVisitor buildVisitor(
            @NotNull ProblemsHolder holder,
            boolean isOnTheFly) {

        return new JavaElementVisitor() {
            @Override
            public void visitTypeCastExpression(@NotNull PsiTypeCastExpression expression) {
                super.visitTypeCastExpression(expression);

                PsiTypeElement castTypeElement = expression.getCastType();
                PsiExpression operand = expression.getOperand();
                if (castTypeElement == null || operand == null) return;

                PsiType castType = castTypeElement.getType();
                PsiType operandType = operand.getType();
                if (operandType == null) return;

                // Verifica se é narrowing conversion entre primitivos
                if (!isNarrowingConversion(operandType, castType)) return;

                // Regra 1: não deve ter invocação de método no operando
                if (hasMethodInvocation(operand)) return;

                // Regra 2: não deve ter operação de módulo no operando
                if (hasModuloOperation(operand)) return;

                // Regra 3: se for literal, só marca se o valor estiver fora do range do tipo alvo
                if (operand instanceof PsiLiteralExpression literal) {
                    if (!isOutOfRange(literal, castType)) return;
                }

                holder.registerProblem(
                        expression,
                        "Confusion atom: narrowing type conversion may cause precision loss or unexpected results.",
                        ProblemHighlightType.WARNING,
                        new TypeConversionQuickFix(TypeConversionQuickFix.Mode.ADD_COMMENT),
                        new TypeConversionQuickFix(TypeConversionQuickFix.Mode.CHANGE_TYPE)
                );
            }
        };
    }

    // Mapa de narrowing conversion conforme o algoritmo
    private boolean isNarrowingConversion(PsiType from, PsiType to) {
        String f = from.getCanonicalText();
        String t = to.getCanonicalText();

        return switch (f) {
            case "short" -> t.equals("byte") || t.equals("char");
            case "char"  -> t.equals("byte") || t.equals("short");
            case "int"   -> t.equals("byte") || t.equals("short") || t.equals("char");
            case "long"  -> t.equals("byte") || t.equals("short") || t.equals("char") || t.equals("int");
            case "float" -> t.equals("byte") || t.equals("short") || t.equals("char")
                    || t.equals("int")  || t.equals("long");
            case "double"-> t.equals("byte") || t.equals("short") || t.equals("char")
                    || t.equals("int")  || t.equals("long")  || t.equals("float");
            default -> false;
        };
    }

    // Verifica se há invocação de método no operando (ex: Math.round(x))
    private boolean hasMethodInvocation(PsiExpression expr) {
        if (expr instanceof PsiMethodCallExpression) return true;
        for (PsiElement child : expr.getChildren()) {
            if (child instanceof PsiExpression childExpr && hasMethodInvocation(childExpr)) {
                return true;
            }
        }
        return false;
    }

    // Verifica se há operação de módulo no operando (ex: x % 256)
    private boolean hasModuloOperation(PsiExpression expr) {
        if (expr instanceof PsiBinaryExpression bin) {
            if (bin.getOperationTokenType() == JavaTokenType.PERC) return true;
            return hasModuloOperation(bin.getLOperand())
                    || (bin.getROperand() != null && hasModuloOperation(bin.getROperand()));
        }
        if (expr instanceof PsiParenthesizedExpression paren) {
            PsiExpression inner = paren.getExpression();
            return inner != null && hasModuloOperation(inner);
        }
        return false;
    }

    // Verifica se o valor literal está fora do range do tipo alvo
    private boolean isOutOfRange(PsiLiteralExpression literal, PsiType targetType) {
        Object value = literal.getValue();
        if (!(value instanceof Number num)) return false;

        double d = num.doubleValue();
        return switch (targetType.getCanonicalText()) {
            case "byte"  -> d < Byte.MIN_VALUE    || d > Byte.MAX_VALUE;
            case "short" -> d < Short.MIN_VALUE   || d > Short.MAX_VALUE;
            case "char"  -> d < Character.MIN_VALUE || d > Character.MAX_VALUE;
            case "int"   -> d < Integer.MIN_VALUE || d > Integer.MAX_VALUE;
            case "long"  -> d < Long.MIN_VALUE    || d > Long.MAX_VALUE;
            case "float" -> d < -Float.MAX_VALUE  || d > Float.MAX_VALUE;
            default -> false;
        };
    }
}