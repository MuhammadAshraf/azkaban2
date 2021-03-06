package azkaban.execapp;

import java.io.IOException;
import java.util.HashMap;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.ObjectName;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import azkaban.executor.ConnectorParams;
import azkaban.utils.JSONUtils;
import azkaban.webapp.servlet.AzkabanServletContextListener;
import azkaban.webapp.servlet.HttpRequestUtils;

public class JMXHttpServlet extends HttpServlet implements ConnectorParams {
	private static final long serialVersionUID = -3085603824826446270L;
	private static final Logger logger = Logger.getLogger(JMXHttpServlet.class);
	private AzkabanExecutorServer server;
	
	public void init(ServletConfig config) throws ServletException {
		server = (AzkabanExecutorServer) config.getServletContext().getAttribute(AzkabanServletContextListener.AZKABAN_SERVLET_CONTEXT_KEY);
	}
	
	public boolean hasParam(HttpServletRequest request, String param) {
		return HttpRequestUtils.hasParam(request, param);
	}
	
	public String getParam(HttpServletRequest request, String name) throws ServletException {
		return HttpRequestUtils.getParam(request, name);
	}
	
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		HashMap<String,Object> ret = new HashMap<String,Object>();

		if (hasParam(req, JMX_GET_MBEANS)) {
			ret.put("mbeans", server.getMbeanNames());
		}
		else if (hasParam(req, JMX_GET_ALL_MBEAN_ATTRIBUTES)) {
			if (!hasParam(req, JMX_MBEAN)) {
				ret.put("error", "Parameters 'mbean' must be set");
			}
			else {
				String mbeanName = getParam(req, JMX_MBEAN);
				try {
					ObjectName name = new ObjectName(mbeanName);
					MBeanInfo info = server.getMBeanInfo(name);
					
					MBeanAttributeInfo[] mbeanAttrs = info.getAttributes();
					HashMap<String, Object> attributes = new HashMap<String,Object>();

					for (MBeanAttributeInfo attrInfo: mbeanAttrs) {
						Object obj = server.getMBeanAttribute(name, attrInfo.getName());
						attributes.put(attrInfo.getName(), obj);
					}
					
					ret.put("attributes", attributes);
				} catch (Exception e) {
					logger.error(e);
					ret.put("error", "'" + mbeanName + "' is not a valid mBean name");
				}
			}
		}

		JSONUtils.toJSON(ret, resp.getOutputStream(), true);
	}
}
