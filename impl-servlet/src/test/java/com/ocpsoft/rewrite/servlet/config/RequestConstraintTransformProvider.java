/*
 * Copyright 2011 <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ocpsoft.rewrite.servlet.config;

import javax.servlet.ServletContext;

import com.ocpsoft.rewrite.bind.Evaluation;
import com.ocpsoft.rewrite.config.Configuration;
import com.ocpsoft.rewrite.config.ConfigurationBuilder;
import com.ocpsoft.rewrite.config.Direction;
import com.ocpsoft.rewrite.context.EvaluationContext;
import com.ocpsoft.rewrite.event.Rewrite;
import com.ocpsoft.rewrite.param.Constraint;
import com.ocpsoft.rewrite.param.Transform;
import com.ocpsoft.rewrite.servlet.http.event.HttpServletRewrite;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * 
 */
public class RequestConstraintTransformProvider extends HttpConfigurationProvider
{
   private final Constraint<String> uppercaseOnly = new Constraint<String>() {
      @Override
      public boolean isSatisfiedBy(Rewrite event, EvaluationContext context, String value)
      {
         return value.matches("[A-Z]+");
      }
   };

   private final Transform<String> toLowercase = new Transform<String>() {
      @Override
      public String transform(Rewrite event, EvaluationContext context, String value)
      {
         return value.toLowerCase();
      }
   };

   @Override
   public int priority()
   {
      return 0;
   }

   @Override
   public Configuration getConfiguration(final ServletContext context)
   {
      Configuration config = ConfigurationBuilder
               .begin()

               .defineRule()
               .when(Direction.isOutbound().and(Path.matches("/outbound/{3}").where("3").transformedBy(toLowercase)))
               .perform(Substitute.with("/outbound/{3}"))

               .defineRule()
               .when(Path.matches("/constraint/{1}/{2}").where("1").constrainedBy(uppercaseOnly).where("2")
                        .constrainedBy(uppercaseOnly).transformedBy(toLowercase))
               .perform(new HttpOperation() {

                  @Override
                  public void performHttp(final HttpServletRewrite event, final EvaluationContext context)
                  {
                     String one = ((String) Evaluation.property("1").retrieve(event, context));
                     String two = ((String) Evaluation.property("2").retrieve(event, context));
                     String three = event.getResponse().encodeRedirectURL(event.getContextPath() + "/outbound/THREE");

                     Response.addHeader("one", one).perform(event, context);
                     Response.addHeader("two", two).perform(event, context);
                     Response.addHeader("three", three).perform(event, context);

                     SendStatus.code(211).perform(event, context);
                  }
               });

      return config;
   }
}