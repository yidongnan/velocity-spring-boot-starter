package net.devh.springboot.autoconfigure.velocity.view;

import net.devh.springboot.autoconfigure.velocity.spring.web.VelocityLayoutViewResolver;
import net.devh.springboot.autoconfigure.velocity.spring.web.VelocityView;

/**
 * User: Michael Chen
 * Email: yidongnan@gmail.com
 * Date: 16-2-20
 */
public class EmbeddedVelocityLayoutViewResolver extends VelocityLayoutViewResolver {

    private String toolboxConfigLocation;

    @Override
    protected void initApplicationContext() {
        if (this.toolboxConfigLocation != null) {
            if (VelocityView.class.equals(getViewClass())) {
                this.logger.info("Using EmbeddedVelocityLayoutToolboxView instead of "
                        + "default VelocityView due to specified toolboxConfigLocation");
                setViewClass(EmbeddedVelocityLayoutToolboxView.class);
            }
        }
        super.initApplicationContext();
    }

    @Override
    public void setToolboxConfigLocation(String toolboxConfigLocation) {
        super.setToolboxConfigLocation(toolboxConfigLocation);
        this.toolboxConfigLocation = toolboxConfigLocation;
    }
}
