/*
 * #%L restdoc-doclet %% Copyright (C) 2012 IG Group %% Licensed under the
 * Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable
 * law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License. #L%
 */
package com.iggroup.oss.restdoclet.doclet.type.builder;

import static com.iggroup.oss.restdoclet.doclet.util.AnnotationUtils.elementValue;
import static com.iggroup.oss.restdoclet.doclet.util.AnnotationUtils.isAnnotated;
import static com.iggroup.oss.restdoclet.doclet.util.AnnotationUtils.parseValueAnnotation;

import java.util.ArrayList;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.iggroup.oss.restdoclet.doclet.type.BodyParameter;
import com.iggroup.oss.restdoclet.doclet.type.Method;
import com.iggroup.oss.restdoclet.doclet.type.ModelParameter;
import com.iggroup.oss.restdoclet.doclet.type.PathParameter;
import com.iggroup.oss.restdoclet.doclet.type.RequestParameter;
import com.iggroup.oss.restdoclet.doclet.type.ResponseParameter;
import com.iggroup.oss.restdoclet.doclet.type.RestParameter;
import com.iggroup.oss.restdoclet.doclet.type.Uri;
import com.iggroup.oss.restdoclet.doclet.util.DocTypeUtils;
import com.iggroup.oss.restdoclet.doclet.util.DocletUtils;
import com.iggroup.oss.restdoclet.doclet.util.NameValuePair;
import com.iggroup.oss.restdoclet.doclet.util.ParameterNamePredicate;
import com.iggroup.oss.restdoclet.doclet.util.RequestMappingParamsParser;
import com.sun.javadoc.AnnotationValue;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.ParamTag;
import com.sun.javadoc.Parameter;

/**
 * This class populates a Method class from JavaDoc types.
 */
public class MethodBuilder {

   /**
    * Logger
    */
   private static final Logger LOG = Logger.getLogger(MethodBuilder.class);

   /**
    * Populate a Method type
    * 
    * @param method type to populate
    * @param methodDoc method doc
    * @param baseUri controller base uri
    * @return populated type
    */
   public Method build(Method method, final MethodDoc methodDoc,
                       final String baseUri) {

      assert method != null;
      assert methodDoc != null;

      LOG.info("Initialising method: " + methodDoc);
      MethodDoc md = DocletUtils.findMethodDocumentation(methodDoc);

      initName(method, methodDoc);
      initRequestMethod(method, methodDoc);
      initProduces(method, methodDoc);
      initConsumes(method, methodDoc);
      initHeaders(method, methodDoc);
      initJavadoc(method, md);
      initRequestParams(method, methodDoc.parameters(), md.paramTags());
      initPathParams(method, methodDoc.parameters(), md.paramTags());
      initModelParams(method, methodDoc.parameters(), md.paramTags());
      initBodyParams(method, methodDoc.parameters(), md.paramTags());
      initRestParams(method, methodDoc, baseUri);
      initResponseParams(method, methodDoc);

      return method;
   }

   /**
    * Initialises the name of this method.
    * 
    * @param methodDoc the method's Java documentation object.
    */
   private void initName(Method method, final MethodDoc methodDoc) {
      method.setName(methodDoc.name());
      LOG.debug(methodDoc.name());
   }

   /**
    * Initialises the HTTP request-method this method is mapped to.
    * 
    * @param methodDoc the method's Java documentation object.
    */
   private void initRequestMethod(Method method, final MethodDoc methodDoc) {
      final AnnotationValue value =
         elementValue(methodDoc, RequestMapping.class, "method");

      LOG.debug(method.getName());

      if (value == null) {
         /* default */
         LOG.debug("No method found.... defaulting to GET");
         method.setRequestMethod(RequestMethod.GET.toString());
      } else {
         /*
          * Java 1.6: AnnotationValue.toString() returns qualified classname
          * example:
          * org.springframework.web.bind.annotation.RequestMethod.GET Java
          * 1.5: AnnotationValue.toString() returns simple classname example:
          * GET
          */
         if (value.toString().contains(".")) {
            method.setRequestMethod(value.toString().substring(
               value.toString().lastIndexOf(".") + 1));
         } else {
            method.setRequestMethod(value.toString());
         }
      }
      LOG.debug(method.getRequestMethod());
   }

   /**
    * Initialises the documentation of this method.
    * 
    * @param methodDoc the method's Java documentation object.
    */
   private void initJavadoc(Method method, final MethodDoc methodDoc) {

      LOG.debug(method.getName());

      method.setJavadoc(DocletUtils.preserveJavadocFormatting(methodDoc
         .commentText()));

   }

   private void initProduces(Method method, final MethodDoc methodDoc) {

      final AnnotationValue value =
         elementValue(methodDoc, RequestMapping.class, "produces");

      LOG.debug(method.getName());

      if (value != null) {
         method.setProduces(value.toString().replace("\"", ""));
         LOG.debug("PRODUCES " + method.getProduces());
      }

   }

   private void initConsumes(Method method, final MethodDoc methodDoc) {
      final AnnotationValue value =
         elementValue(methodDoc, RequestMapping.class, "consumes");

      LOG.debug(method.getName());

      if (value != null) {
         method.setConsumes(value.toString().replace("\"", ""));
         LOG.debug("CONSUMES " + method.getConsumes());
      }
   }

   private void initHeaders(Method method, final MethodDoc methodDoc) {
      final AnnotationValue value =
         elementValue(methodDoc, RequestMapping.class, "headers");

      LOG.debug(method.getName());

      if (value != null) {
         method.setHeaders(value.toString().replace("\"", ""));
         LOG.debug("HEADERS " + method.getHeaders());
      }
   }

   /**
    * Initialises the request-parameters of this method.
    * 
    * @param params the method's parameters.
    * @param tags the parameters' Java documentation tags.
    */
   private void initRequestParams(Method method, final Parameter[] params,
                                  final ParamTag[] tags) {
      LOG.debug(method.getName());
      ArrayList<RequestParameter> requestParams =
         new ArrayList<RequestParameter>();
      for (Parameter param : params) {
         if (isAnnotated(param, RequestParam.class)) {
            requestParams.add(new RequestParameterBuilder().build(
               new RequestParameter(), param, tags));
         }
      }
      method.setRequestParams(requestParams);
   }

   /**
    * Initialises the path-parameters of this method.
    * 
    * @param params the method's path parameters.
    * @param tags the parameters' Java documentation tags.
    */
   private void initPathParams(Method method, final Parameter[] params,
                               final ParamTag[] tags) {
      LOG.debug(method.getName());
      ArrayList<PathParameter> pathParams = new ArrayList<PathParameter>();
      for (Parameter param : params) {
         if (isAnnotated(param, PathVariable.class)) {
            pathParams.add(new PathParameterBuilder().build(
               new PathParameter(), param, tags));
         }
      }
      method.setPathParams(pathParams);
   }

   /**
    * Initialises the model-parameters of this method.
    * 
    * @param params the method's parameters.
    * @param tags the parameters' Java documentation tags.
    */
   private void initModelParams(Method method, final Parameter[] params,
                                final ParamTag[] tags) {
      LOG.debug(method.getName());
      ArrayList<ModelParameter> modelParams = new ArrayList<ModelParameter>();
      for (Parameter param : params) {
         if (isAnnotated(param, ModelAttribute.class)) {
            modelParams.add(new ModelParameterBuilder().build(
               new ModelParameter(), param, tags));
         }
      }
      method.setModelParams(modelParams);
   }

   /**
    * Initialises the request body parameters of this method.
    * 
    * @param params the method's parameters.
    * @param tags the parameters' Java documentation tags.
    */
   private void initBodyParams(Method method, final Parameter[] params,
                               final ParamTag[] tags) {
      LOG.debug(method.getName());
      ArrayList<BodyParameter> bodyParams = new ArrayList<BodyParameter>();
      for (Parameter param : params) {
         if (isAnnotated(param, RequestBody.class)) {
            LOG.debug("YYYYYYYYYYYYYYYYYY");
            bodyParams.add(new BodyParameterBuilder().build(
               new BodyParameter(), param, tags));
         }
      }
      method.setBodyParams(bodyParams);
   }

   /**
    * Initialises the REST-parameters of this method.
    * 
    * @param method method to initialise
    * @param methodDoc the method's Java documentation object.
    * @param baseUri the controller base uri
    */
   private void initRestParams(Method method, final MethodDoc methodDoc,
                               final String baseUri) {

      LOG.debug(method.getName());
      ArrayList<RestParameter> restParams = new ArrayList<RestParameter>();

      for (NameValuePair pair : new RequestMappingParamsParser(elementValue(
         methodDoc, RequestMapping.class, "params")).parse()) {

         final Predicate predicate =
            new ParameterNamePredicate(pair.getName());

         if (!CollectionUtils.exists(method.getRequestParams(), predicate)) {
            LOG.debug(pair.getName() + " - " + pair.getValue());
            restParams.add(new RestParameter(pair));
         }

      }

      AnnotationValue urlAnnotation =
         elementValue(methodDoc, RequestMapping.class, "value");
      if (urlAnnotation != null) {
         Boolean deprecatedMatch = false;
         String[] methodUris = parseValueAnnotation(urlAnnotation);
         String[] deprecatedURIs = DocTypeUtils.getDeprecatedURIs(methodDoc);

         for (final String uri : methodUris) {
            LOG.debug("uri:" + baseUri + uri);
            boolean deprecated = false;
            if (deprecatedURIs != null) {
               for (final String deprecatedUri : deprecatedURIs) {
                  LOG.debug("deprecated:" + deprecatedUri);
                  if (StringUtils.equals(deprecatedUri, uri)) {
                     LOG.debug("=DEPRECATED");
                     deprecated = true;
                     deprecatedMatch = true;
                     break;
                  }
               }
            }
            method.getUris().add(new Uri(baseUri + uri, deprecated));
         }

         if (deprecatedURIs != null && !deprecatedMatch) {
            LOG.warn("Deprecated URI tag on method " + methodDoc.name()
               + " does not match any service URIs.");
         }
      }

      method.setRestParams(restParams);
   }

   private void initResponseParams(Method method, final MethodDoc methodDoc) {

      ArrayList<ResponseParameter> responseParams =
         new ArrayList<ResponseParameter>();
      // Add return type
      if (methodDoc.returnType() != null) {
         responseParams.add(new ResponseParameter(DocTypeUtils
            .getTypeName(methodDoc.returnType()), DocTypeUtils
            .getTypeName(methodDoc.returnType()), DocletUtils
            .preserveJavadocFormatting(DocTypeUtils.getReturnDoc(methodDoc))));
      }
      // Add any checked exceptions
      for (ClassDoc exceptionDoc : methodDoc.thrownExceptions()) {
         responseParams.add(new ResponseParameter(DocTypeUtils
            .getTypeName(exceptionDoc),
            DocTypeUtils.getTypeName(exceptionDoc), DocletUtils
            .preserveJavadocFormatting(DocTypeUtils
               .getTypeDoc(exceptionDoc))));
      }
      method.setResponseParams(responseParams);

   }

}
