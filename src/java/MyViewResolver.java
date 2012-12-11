
import java.util.Locale;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Paul
 */
public class MyViewResolver implements ViewResolver {

    private ViewResolver tilesResolver;
    private ViewResolver jspResolver;

    public void setJspResolver(ViewResolver jspResolver) {
        this.jspResolver = jspResolver;
    }

    public void setTilesResolver(ViewResolver tilesResolver) {
        this.tilesResolver = tilesResolver;
    }

    @Override
    public View resolveViewName(String viewName, Locale locale) throws Exception {
        System.out.println(viewName);
        if (viewName.contains("php")) {
            return tilesResolver.resolveViewName(viewName, locale);
        } else {
            return jspResolver.resolveViewName(viewName, locale);
        }
    }

   
}
