package br.ufc.aocrefactor.quickfix;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

public class WrapWithParenthesesQuickFix implements LocalQuickFix {

    @Override
    public @NotNull String getName() {
        return "Add parentheses for clarity";
    }

    @Override
    public @NotNull String getFamilyName() {
        return getName();
    }

    @Override
    public void applyFix(@NotNull Project project,
                         @NotNull ProblemDescriptor descriptor) {

        PsiExpression expression = (PsiExpression) descriptor.getPsiElement();

        PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);

        // Envolve a expressão em parênteses: expr → (expr)
        PsiExpression wrapped = factory.createExpressionFromText(
                "(" + expression.getText() + ")",
                expression
        );

        expression.replace(wrapped);
    }
}