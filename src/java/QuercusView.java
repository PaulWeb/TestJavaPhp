/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
import com.caucho.quercus.QuercusContext;
import com.caucho.quercus.QuercusDieException;
import com.caucho.quercus.QuercusErrorException;
import com.caucho.quercus.QuercusExitException;
import com.caucho.quercus.QuercusLineRuntimeException;
import com.caucho.quercus.QuercusRequestAdapter;
import com.caucho.quercus.QuercusRuntimeException;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.JavaValue;
import com.caucho.quercus.env.QuercusValueException;
import com.caucho.quercus.env.StringValue;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.page.QuercusPage;
import com.caucho.util.L10N;
import com.caucho.vfs.FilePath;
import com.caucho.vfs.Path;
import com.caucho.vfs.StreamImpl;
import com.caucho.vfs.VfsStream;
import com.caucho.vfs.WriteStream;
import com.caucho.vfs.WriterStreamImpl;
import java.io.FileNotFoundException;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.view.AbstractUrlBasedView;
/**
 *
 * @author Paul
 */
public class QuercusView extends AbstractUrlBasedView
{
  private static final L10N L = new L10N(QuercusView.class);

  private static final Logger log = Logger.getLogger(QuercusView.class.getName());
  protected QuercusContext _quercus;
  protected ServletContext _servletContext;

  protected void initServletContext(ServletContext servletContext)
  {
    this._servletContext = servletContext;

    checkServletAPIVersion();

    getQuercus().setPwd(new FilePath(this._servletContext.getRealPath("/")));

    getQuercus().init();
  }

  protected void checkServletAPIVersion()
  {
    int major = this._servletContext.getMajorVersion();
    int minor = this._servletContext.getMinorVersion();

    if ((major < 2) || ((major == 2) && (minor < 4)))
      throw new QuercusRuntimeException(L.l("Quercus requires Servlet API 2.4+."));
  }

  protected void renderMergedOutputModel(Map model, HttpServletRequest request, HttpServletResponse response)
    throws Exception
  {
    Env env = null;
    WriteStream ws = null;
    try
    {
      Path path = getPath(request);
      QuercusPage page;
      try {
        page = getQuercus().parse(path);
      }
      catch (FileNotFoundException ex)
      {
        log.log(Level.FINER, ex.toString(), ex);

        response.sendError(404);

        return;
      }
      StreamImpl out;
      try
      {
        out = new VfsStream(null, response.getOutputStream());
      }
      catch (IllegalStateException e) {
        WriterStreamImpl writer = new WriterStreamImpl();
        writer.setWriter(response.getWriter());

        out = writer;
      }

      ws = new WriteStream(out);

      ws.setNewlineString("\n");
//Date hj=new Date();

      QuercusContext quercus = getQuercus();
      quercus.setServletContext(this._servletContext);
      System.out.println(page);
      env = quercus.createEnv(page, ws, request, response);
      System.out.println(env);
      for (Iterator i$ = model.entrySet().iterator(); i$.hasNext(); ) { Object entryObj = i$.next();
        Map.Entry entry = (Map.Entry)entryObj;
        System.out.println((String)entry.getKey()+" "+entry.getValue());
       // com.caucho.quercus.program.JavaClassDef.create(null, DEFAULT_CONTENT_TYPE, null).
        // Value r=new JavaValue(env,"klj",);
        env.setGlobalValue((String)entry.getKey(), env.wrapJava(entry.getValue()));
      }
      try
      {
        env.start();
        //
        env.setGlobalValue("request", env.wrapJava(request));
        env.setScriptGlobal("response", response);
        env.setScriptGlobal("servletContext", this._servletContext);

        StringValue prepend = quercus.getIniValue("auto_prepend_file").toStringValue(env);

        if (prepend.length() > 0) {
          Path prependPath = env.lookup(prepend);

          if (prependPath == null) {
            env.error(L.l("auto_prepend_file '{0}' not found.", prepend));
          } else {
            QuercusPage prependPage = getQuercus().parse(prependPath);
            prependPage.executeTop(env);
          }
        }

        env.execute();

        StringValue append = quercus.getIniValue("auto_append_file").toStringValue(env);

        if (append.length() > 0) {
          Path appendPath = env.lookup(append);

          if (appendPath == null) {
            env.error(L.l("auto_append_file '{0}' not found.", append));
          } else {
            QuercusPage appendPage = getQuercus().parse(appendPath);
            appendPage.executeTop(env);
          }
        }
      }
      catch (QuercusExitException e)
      {
        throw e;
      }
      catch (QuercusErrorException e) {
        throw e;
      }
      catch (QuercusLineRuntimeException e) {
        log.log(Level.FINE, e.toString(), e);
      }
      catch (QuercusValueException e)
      {
        log.log(Level.FINE, e.toString(), e);

        ws.println(e.toString());
      }
      catch (Throwable e)
      {
        if (response.isCommitted()) {
          e.printStackTrace(ws.getPrintWriter());
        }
        ws = null;

        throw e;
      }
      finally {
        if (env != null) {
          env.close();
        }

        if (ws != null)
          ws.close();
        if(quercus!=null)
            quercus.close();
      }
    }
    catch (QuercusDieException e)
    {
      log.log(Level.FINE, e.toString(), e);
    }
    catch (QuercusExitException e)
    {
      log.log(Level.FINER, e.toString(), e);
    }
    catch (QuercusErrorException e)
    {
      log.log(Level.FINE, e.toString(), e);
    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (Throwable e) {
      throw new ServletException(e);
    }
  }

  Path getPath(HttpServletRequest req)
  {
    String scriptPath = getUrl();
    String pathInfo = QuercusRequestAdapter.getPagePathInfo(req);

    Path pwd = new FilePath(System.getProperty("user.dir"));

    Path path = pwd.lookup(req.getRealPath(scriptPath));

    if (path.isFile())
      return path;
    String fullPath;   
    if (pathInfo != null)
      fullPath = scriptPath + pathInfo;
    else {
      fullPath = scriptPath;
    }
    return pwd.lookup(req.getRealPath(fullPath));
  }

  protected QuercusContext getQuercus()
  {
    synchronized (this) {
      if (this._quercus == null) {
        this._quercus = new QuercusContext();
         this._quercus.start();
      }
    }
    return this._quercus;
  }

  public void destroy()
  {
    this._quercus.close();
  }
}
