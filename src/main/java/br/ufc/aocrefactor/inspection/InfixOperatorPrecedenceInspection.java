package br.ufc.aocrefactor.inspection;

import com.intellij.codeInspection.*;
import com.intellij.psi.*;
import com.intellij.psi.tree.IElementType;
import br.ufc.aocrefactor.quickfix.WrapWithParenthesesQuickFix;
import org.jetbrains.annotations.NotNull;

public class InfixOperatorPrecedenceInspection extends AbstractBaseJavaLocalInspectionTool {

    @Override
    public @NotNull PsiElementVisitor buildVisitor(
            @NotNull ProblemsHolder holder,
            boolean isOnTheFly) {

        return new JavaElementVisitor() {
            @Override
            public void visitPolyadicExpression(@NotNull PsiPolyadicExpression expression) {
                super.visitPolyadicExpression(expression);
                checkExpression(expression, holder);
            }

            @Override
            public void visitBinaryExpression(@NotNull PsiBinaryExpression expression) {
                super.visitBinaryExpression(expression);
                checkExpression(expression, holder);
            }
        };
    }

    private void checkExpression(PsiExpression expression, ProblemsHolder holder) {
        IElementType op = getOperator(expression);
        if (op == null) return;

        // Pega o pai ignorando parênteses
        PsiElement parent = expression.getParent();

        // Se já está entre parênteses, não é átomo de confusão
        if (parent instanceof PsiParenthesizedExpression) return;

        IElementType parentOp = getOperator(parent);
        if (parentOp == null) return;

        // --- CASO ARITMÉTICO ---
        // Se o operador atual é *, / ou % e o pai é + ou -
        if (isHighPrecedenceArithmetic(op) && isLowPrecedenceArithmetic(parentOp)) {
            // Verifica se não é concatenação de strings
            if (isStringConcatenation(parent)) return;

            holder.registerProblem(
                    expression,
                    "Átomo de confusão: precedência de operador aritmético. Adicione parênteses para clareza.",
                    ProblemHighlightType.WARNING,
                    new WrapWithParenthesesQuickFix()
            );
        }

        // --- CASO LÓGICO ---
        // Se o operador atual é && e o pai é ||, ou atual é || e o pai é &&
        if (isLogicalOperator(op) && isLogicalOperator(parentOp) && !op.equals(parentOp)) {
            holder.registerProblem(
                    expression,
                    "Átomo de confusão: precedência de operador lógico. Adicione parênteses para clareza.",
                    ProblemHighlightType.WARNING,
                    new WrapWithParenthesesQuickFix()
            );
        }
    }

    // Retorna o operador da expressão, seja binária ou poliádica
    private IElementType getOperator(PsiElement element) {
        if (element instanceof PsiBinaryExpression bin) {
            return bin.getOperationTokenType();
        }
        if (element instanceof PsiPolyadicExpression poly) {
            return poly.getOperationTokenType();
        }
        return null;
    }

    // *, / ou %
    private boolean isHighPrecedenceArithmetic(IElementType op) {
        return op == JavaTokenType.ASTERISK
                || op == JavaTokenType.DIV
                || op == JavaTokenType.PERC;
    }

    // + ou -
    private boolean isLowPrecedenceArithmetic(IElementType op) {
        return op == JavaTokenType.PLUS
                || op == JavaTokenType.MINUS;
    }

    // && ou ||
    private boolean isLogicalOperator(IElementType op) {
        return op == JavaTokenType.ANDAND
                || op == JavaTokenType.OROR;
    }

    // Verifica se a expressão pai é uma concatenação de strings (tipo String)
    private boolean isStringConcatenation(PsiElement element) {
        if (element instanceof PsiBinaryExpression bin) {
            PsiType type = bin.getType();
            return type != null && type.equalsToText("java.lang.String");
        }
        if (element instanceof PsiPolyadicExpression poly) {
            PsiType type = poly.getType();
            return type != null && type.equalsToText("java.lang.String");
        }
        return false;
    }
}