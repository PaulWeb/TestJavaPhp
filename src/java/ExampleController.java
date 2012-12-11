
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 * This conr=troller is designed for demonstrate php and spring bundle
 * @see http://www.mkyong.com/spring-mvc/spring-mvc-hello-world-example/
 * @author Paul Shishakov
 */
public class ExampleController extends AbstractController{

    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest hsr, HttpServletResponse hsr1) throws Exception {
        
                ModelAndView model = new ModelAndView("testphp");
		model.addObject("msg", "PHP and SPRING ase together!");
 
		return model;
    }
    
}
