package com.adrianguenter.php_declaration_hints;

import com.adrianguenter.php_declaration_hints.config.MethodProviderConfig;
import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.codeInsight.template.Template;
import com.intellij.codeInsight.template.TemplateManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.PhpIcons;
import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpModifier;
import com.jetbrains.php.lang.psi.elements.impl.GroupStatementImpl;
import com.jetbrains.php.lang.psi.elements.impl.MethodImpl;
import com.jetbrains.php.lang.psi.elements.impl.PhpClassFieldsListImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MethodCompletionContributor
        extends CompletionContributor {
    public MethodCompletionContributor() {
        this.extend(
                CompletionType.BASIC,
                PlatformPatterns.psiElement().withLanguage(PhpLanguage.INSTANCE),
                new CompletionProvider<>() {
                    @Override
                    protected void addCompletions(
                            @NotNull CompletionParameters parameters,
                            @NotNull ProcessingContext context,
                            @NotNull CompletionResultSet resultSet
                    ) {
                        var element = parameters.getOriginalPosition();
                        if (element == null) {
                            return;
                        }

                        var file = element.getContainingFile();
                        var project = file.getProject();
                        var cache = project.getService(ConfigCache.class);
                        var templateManager = TemplateManager.getInstance(project);

                        var phpClass = PsiTreeUtil.getParentOfType(element, PhpClass.class);
                        if (phpClass == null) {
                            return;
                        }

                        MethodImpl method;
                        if (element.getParent() instanceof MethodImpl) {
                            method = (MethodImpl) element.getParent();
                        } else if (element instanceof PsiWhiteSpace && element.getPrevSibling() instanceof MethodImpl) {
                            /// Capture the method if we're immediately after one that's unnamed.
                            /// E.g.: `public function <caret>`, but not `public function foo(){} <caret>`
                            method = ((MethodImpl) element.getPrevSibling()).getNameIdentifier() == null
                                    ? (MethodImpl) element.getPrevSibling()
                                    : null;
                        } else if (
                                element instanceof PsiWhiteSpace
                                        && element.getPrevSibling() instanceof PhpClassFieldsListImpl
                                        && (!(element.getPrevSibling().getLastChild() instanceof LeafPsiElement)
                                        || !((LeafPsiElement) element.getPrevSibling().getLastChild()).getElementType().toString().equals("semicolon"))
                        ) {
                            ///  TODO: Fix `public int $a, <caret>` (should not hint!)
                            return;
                        } else if (element.getParent() instanceof PhpClass) {
                            method = null;
                        } else {
                            return;
                        }

                        var config = cache.getPhpFileConfig(file.getVirtualFile());
                        if (config == null || !config.classes().containsKey(phpClass.getFQN())) {
                            return;
                        }

                        var classConfig = config.classes().get(phpClass.getFQN());

                        for (var methodProvider : classConfig.methodProviders().entrySet()) {
                            var methodName = methodProvider.getKey();
                            var methodConfig = methodProvider.getValue();

                            if (phpClass.findOwnMethodByName(methodName) != null) {
                                continue;
                            }

                            var accessLevel = methodConfig.accessLevel() != null
                                    ? methodConfig.accessLevel()
                                    : PhpModifier.Access.PUBLIC;

                            if (method != null) {
                                if (method.isStatic() && !methodConfig.isStatic()) {
                                    continue;
                                }

                                if (!accessLevel.equals(method.getAccess())) {
                                    continue;
                                }
                            }

                            String paramsText;
                            if (methodConfig.params() != null) {
                                 paramsText = String.join(
                                        ", ",
                                        methodConfig.params().entrySet().stream().map(
                                                item -> {
                                                    var paramName = item.getKey();
                                                    var paramConfig = item.getValue();

                                                    return String.format(
                                                            "%s %s$%s%s",
                                                            paramConfig.type() != null
                                                                    ? paramConfig.type()
                                                                    : "mixed",
                                                            paramConfig.isVariadic()
                                                                    ? "..."
                                                                    : "",
                                                            paramName,
                                                            paramConfig.defaultValue() != null
                                                                    ? " = " + paramConfig.defaultValue()
                                                                    : ""
                                                    );
                                                }
                                        ).toList()
                                );
                            }
                            else {
                                paramsText = "";
                            }

                            var accessLevelText = accessLevel.toString();

                            var staticText = methodConfig.isStatic()
                                    ? " static"
                                    : "";

                            var returnTypeText = methodConfig.returnType() != null
                                    ? methodConfig.returnType()
                                    : "void";

                            LookupElementBuilder lookupElement = LookupElementBuilder
                                    .create(methodName)
                                    .withIcon(PhpIcons.METHOD)
                                    .withPresentableText(accessLevelText + staticText + " function " + methodName)
                                    .withTailText("(" + paramsText + ")", true)
                                    .withTypeText(returnTypeText, true)
                                    .withInsertHandler((insertionContext, item) -> {
                                        Editor editor = insertionContext.getEditor();
                                        Document document = editor.getDocument();

                                        var existingDocComment = method != null ? method.getDocComment() : null;
                                        var groupStatementText = getGroupStatementText(method, methodConfig);

                                        TextRange methodTextRange = getTextRange(method, existingDocComment);

                                        if (methodTextRange != null) {
                                            document.deleteString(methodTextRange.getStartOffset(), methodTextRange.getEndOffset());
                                            editor.getCaretModel().moveToOffset(methodTextRange.getStartOffset());
                                        } else {
                                            document.deleteString(insertionContext.getStartOffset(), insertionContext.getTailOffset());
                                            editor.getCaretModel().moveToOffset(insertionContext.getStartOffset());
                                        }

                                        var attributesText = "";
                                        if (method != null) {
                                            attributesText = String.join(
                                                    "\n",
                                                    method.getAttributes()
                                                            .stream().map(attr -> "#[" + attr.getText() + "]")
                                                            .toList()
                                            ) + "\n";
                                        }

                                        var commentText = "";
                                        if (existingDocComment != null) {
                                            commentText = existingDocComment.getText();
                                        } else if (methodConfig.comment() != null) {
                                            commentText = methodConfig.comment();
                                        }

                                        Template template = templateManager.createTemplate(
                                                "",
                                                "",
                                                String.format(
                                                        "%s%s%s%s function %s(%s): %s\n{\n%s}",
                                                        commentText,
                                                        attributesText,
                                                        accessLevelText,
                                                        staticText,
                                                        methodName,
                                                        paramsText,
                                                        returnTypeText,
                                                        !groupStatementText.isEmpty()
                                                                ? groupStatementText
                                                                : "$END$\n"
                                                )
                                        );
                                        template.setToShortenLongNames(true);
                                        template.setToReformat(true);

                                        templateManager.startTemplate(editor, template);
                                    });

                            resultSet.addElement(PrioritizedLookupElement.withPriority(
                                    lookupElement,
                                    1000.0
                            ));
                        }
                    }

                    private static @Nullable TextRange getTextRange(
                            MethodImpl method,
                            PhpDocComment existingDocComment
                    ) {
                        @Nullable TextRange methodTextRange;
                        if (method == null) {
                            methodTextRange = null;
                        } else if (existingDocComment != null) {
                            methodTextRange = new TextRange(
                                    existingDocComment.getTextOffset(),
                                    method.getTextRange().getEndOffset()
                            );
                        } else {
                            methodTextRange = method.getTextRange();
                        }
                        return methodTextRange;
                    }

                    private static @NotNull String getGroupStatementText(
                            MethodImpl method,
                            MethodProviderConfig methodConfig
                    ) {
                        var methodLastChild = method != null ? method.getLastChild() : null;
                        var existingGroupStatement = methodLastChild instanceof GroupStatementImpl
                                ? methodLastChild
                                : null;

                        var groupStatementText = "";
                        if (existingGroupStatement != null) {
                            groupStatementText = existingGroupStatement.getText()
                                    /// Remove braces
                                    .substring(1, existingGroupStatement.getText().length() - 1)
                                    .trim();
                        }

                        if (groupStatementText.isEmpty() && methodConfig.impl() != null) {
                            groupStatementText = methodConfig.impl();
                        }

                        if (!groupStatementText.contains("$END$")) {
                            groupStatementText += "$END$\n";
                        }
                        return groupStatementText;
                    }
                }
        );
    }
}
