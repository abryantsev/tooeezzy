package xxx.tooe.experiment.jetty

import javax.servlet.http.{HttpServletResponse, HttpServletRequest, HttpServlet}
import javax.servlet.ServletException

class JettyTestServlet()  extends HttpServlet
{
  override def doGet( request: HttpServletRequest, response:HttpServletResponse)
  {

    response.setContentType("text/html")
    response.setStatus(HttpServletResponse.SC_OK)
    response.getWriter.println("<h1>Test Jetty</h1>")
    response.getWriter.println("session=" + request.getSession(true).getId)
  }
}
