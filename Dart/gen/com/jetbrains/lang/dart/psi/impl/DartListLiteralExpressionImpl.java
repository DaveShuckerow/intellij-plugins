// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

// This is a generated file. Not intended for manual editing.
package com.jetbrains.lang.dart.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static com.jetbrains.lang.dart.DartTokenTypes.*;
import com.jetbrains.lang.dart.psi.*;
import com.jetbrains.lang.dart.util.DartPsiImplUtil;

public class DartListLiteralExpressionImpl extends DartClassReferenceImpl implements DartListLiteralExpression {

  public DartListLiteralExpressionImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull DartVisitor visitor) {
    visitor.visitListLiteralExpression(this);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof DartVisitor) accept((DartVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<DartElement> getElementList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, DartElement.class);
  }

  @Override
  @Nullable
  public DartTypeArguments getTypeArguments() {
    return findChildByClass(DartTypeArguments.class);
  }

}
