// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.angular2.codeInsight;

import com.intellij.lang.javascript.inspections.JSMethodCanBeStaticInspection;
import com.intellij.lang.javascript.inspections.JSUnusedGlobalSymbolsInspection;
import com.intellij.lang.javascript.inspections.JSUnusedLocalSymbolsInspection;
import com.intellij.lang.javascript.inspections.UnterminatedStatementJSInspection;
import com.intellij.lang.typescript.inspections.TypeScriptUnresolvedFunctionInspection;
import com.intellij.lang.typescript.inspections.TypeScriptUnresolvedVariableInspection;
import org.angular2.Angular2CodeInsightFixtureTestCase;
import org.angularjs.AngularTestUtil;

import static java.util.Arrays.asList;

public class InspectionsTest extends Angular2CodeInsightFixtureTestCase {
  @Override
  protected String getTestDataPath() {
    return AngularTestUtil.getBaseTestDataPath(getClass()) + "inspections";
  }

  public void testUnusedSymbol() {
    myFixture.enableInspections(JSUnusedGlobalSymbolsInspection.class,
                                JSUnusedLocalSymbolsInspection.class);
    myFixture.configureByFiles("unused.ts", "unused.html", "package.json");
    myFixture.checkHighlighting();
  }

  public void testUnusedSetter() {
    myFixture.enableInspections(JSUnusedGlobalSymbolsInspection.class,
                                JSUnusedLocalSymbolsInspection.class);
    myFixture.configureByFiles("unusedSetter.ts", "unusedSetter.html", "package.json");
    myFixture.checkHighlighting();
  }

  public void testMethodCanBeStatic() {
    myFixture.enableInspections(JSMethodCanBeStaticInspection.class);
    myFixture.configureByFiles("methodCanBeStatic.ts", "methodCanBeStatic.html", "package.json");
    myFixture.checkHighlighting();
  }

  public void testUnterminated() {
    myFixture.enableInspections(UnterminatedStatementJSInspection.class);
    myFixture.configureByFiles("unterminated.ts", "package.json");
    myFixture.checkHighlighting();
  }

  public void testUnusedReference() {
    myFixture.enableInspections(JSUnusedGlobalSymbolsInspection.class,
                                JSUnusedLocalSymbolsInspection.class);
    myFixture.configureByFiles("unusedReference.html", "unusedReference.ts", "package.json");
    myFixture.checkHighlighting();

    for (String attrToRemove : asList("notUsedRef", "anotherNotUsedRef", "notUsedRefWithAttr", "anotherNotUsedRefWithAttr")) {
      AngularTestUtil.moveToOffsetBySignature("<caret>" + attrToRemove, myFixture);
      myFixture.launchAction(myFixture.findSingleIntention("Remove unused variable '" + attrToRemove + "'"));
    }
    myFixture.checkResultByFile("unusedReference.after.html");
  }

  public void testId() {
    myFixture.enableInspections(JSUnusedLocalSymbolsInspection.class,
                                JSUnusedGlobalSymbolsInspection.class);
    myFixture.configureByFiles("object.ts", "package.json");
    myFixture.checkHighlighting();
  }

  public void testPipeAndArgResolution() {
    myFixture.enableInspections(TypeScriptUnresolvedVariableInspection.class,
                                TypeScriptUnresolvedFunctionInspection.class);
    myFixture.configureByFiles("pipeAndArgResolution.html", "lowercase_pipe.ts", "package.json");
    myFixture.checkHighlighting();
  }
}
