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
import net.devh.springboot.autoconfigure.velocity.spring.web.VelocityLayoutView;

import org.apache.velocity.context.Context;
import org.apache.velocity.tools.Scope;
import org.apache.velocity.tools.ToolboxFactory;
import org.apache.velocity.tools.config.XmlFactoryConfiguration;
import org.apache.velocity.tools.view.ViewToolContext;

import java.util.Map;

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
public class EmbeddedVelocityLayoutToolboxView extends VelocityLayoutView {

    @Override
    protected Context createVelocityContext(Map<String, Object> model,
                                            HttpServletRequest request, HttpServletResponse response) throws Exception {
        ViewToolContext velocityContext = new ViewToolContext(
                getVelocityEngine(), request, response, getServletContext());
        velocityContext.putAll(model);
        if (getToolboxConfigLocation() != null) {
            setContextToolbox(velocityContext);
        }
        return velocityContext;
    }

    private void setContextToolbox(
            ViewToolContext velocityContext) {

        XmlFactoryConfiguration cfg = new XmlFactoryConfiguration();
        try {
            cfg.read(EmbeddedVelocityLayoutToolboxView.class.getResource(getToolboxConfigLocation()));
        } catch (Exception e) {
            logger.error("can't found toolbox config file", e);
        }
        ToolboxFactory factory = cfg.createFactory();

        velocityContext.addToolbox(factory.createToolbox(Scope.APPLICATION));
        velocityContext.addToolbox(factory.createToolbox(Scope.REQUEST));
        velocityContext.addToolbox(factory.createToolbox(Scope.SESSION));
    }

}
