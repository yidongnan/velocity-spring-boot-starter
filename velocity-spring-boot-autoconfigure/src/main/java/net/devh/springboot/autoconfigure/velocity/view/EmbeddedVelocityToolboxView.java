/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.devh.springboot.autoconfigure.velocity.view;

import net.devh.springboot.autoconfigure.velocity.spring.web.VelocityToolboxView;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.velocity.context.Context;
import org.apache.velocity.tools.Scope;
import org.apache.velocity.tools.ToolboxFactory;
import org.apache.velocity.tools.config.XmlFactoryConfiguration;
import org.apache.velocity.tools.view.ViewToolContext;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.context.support.ServletContextResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Extended version of {@link VelocityToolboxView} that can load toolbox locations from
 * the classpath as well as the servlet context. This is useful when running in an
 * embedded web server.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 1.2.5
 */
public class EmbeddedVelocityToolboxView extends VelocityToolboxView {

    @Override
    protected Context createVelocityContext(Map<String, Object> model,
                                            HttpServletRequest request, HttpServletResponse response) throws Exception {
        /*org.apache.velocity.tools.view.context.ChainedContext context = new org.apache.velocity.tools.view.context.ChainedContext(
                new VelocityContext(model), getVelocityEngine(), request, response,
                getServletContext());
        if (getToolboxConfigLocation() != null) {
            setContextToolbox(context);
        }
        return context;*/

        // Custom by Michael
        // Create a ViewToolContext instance since ChainedContext is deprecated
        // in Velocity Tools 2.0.
        ViewToolContext velocityContext = new ViewToolContext(
                getVelocityEngine(), request, response, getServletContext());
        velocityContext.putAll(model);
        if (getToolboxConfigLocation() != null) {
            setContextToolbox(velocityContext);
        }
        return velocityContext;
    }

    @SuppressWarnings("unchecked")
    private void setContextToolbox(
            ViewToolContext velocityContext) {
        /*org.apache.velocity.tools.view.ToolboxManager toolboxManager = org.apache.velocity.tools.view.servlet.ServletToolboxManager
                .getInstance(getToolboxConfigFileAwareServletContext(),
                        getToolboxConfigLocation());
        Map<String, Object> toolboxContext = toolboxManager.getToolbox(context);
        context.setToolbox(toolboxContext);*/

        // Custom by Michael
        // Load a Configuration and publish toolboxes to the context when
        // necessary

        XmlFactoryConfiguration cfg = new XmlFactoryConfiguration();
        try {
            cfg.read(new ServletContextResource(getToolboxConfigFileAwareServletContext(), getToolboxConfigLocation()).getURL());
        } catch (IOException e) {
            logger.error("can't found toolbox config file", e);
        }
        ToolboxFactory factory = cfg.createFactory();

        velocityContext.addToolbox(factory.createToolbox(Scope.APPLICATION));
        velocityContext.addToolbox(factory.createToolbox(Scope.REQUEST));
        velocityContext.addToolbox(factory.createToolbox(Scope.SESSION));
    }

    private ServletContext getToolboxConfigFileAwareServletContext() {
        ProxyFactory factory = new ProxyFactory();
        factory.setTarget(getServletContext());
        factory.addAdvice(new GetResourceMethodInterceptor(getToolboxConfigLocation()));
        return (ServletContext) factory.getProxy(getClass().getClassLoader());
    }

    /**
     * {@link MethodInterceptor} to allow the calls to getResourceAsStream() to resolve
     * the toolboxFile from the classpath.
     */
    private static class GetResourceMethodInterceptor implements MethodInterceptor {

        private final String toolboxFile;

        GetResourceMethodInterceptor(String toolboxFile) {
            if (toolboxFile != null && !toolboxFile.startsWith("/")) {
                toolboxFile = "/" + toolboxFile;
            }
            this.toolboxFile = toolboxFile;
        }

        @Override
        public Object invoke(MethodInvocation invocation) throws Throwable {
            if (invocation.getMethod().getName().equals("getResourceAsStream")
                    && invocation.getArguments()[0].equals(this.toolboxFile)) {
                InputStream inputStream = (InputStream) invocation.proceed();
                if (inputStream == null) {
                    try {
                        inputStream = new ClassPathResource(this.toolboxFile,
                                Thread.currentThread().getContextClassLoader())
                                .getInputStream();
                    } catch (Exception ex) {
                        // Ignore
                    }
                }
                return inputStream;
            }
            return invocation.proceed();
        }

    }

}
