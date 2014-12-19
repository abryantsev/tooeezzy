package com.tooe.core.db.graph.load;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;

import com.thinkaurelius.titan.core.TitanFactory;
import com.thinkaurelius.titan.core.TitanGraph;

public class TitanGraphLoadFactory {

	private static Logger logger = Logger.getLogger(TitanGraphLoadFactory.class);

	private String backend;

	private String hostname;

	private String autotype;

  private String srf;

	private Integer connectionTimeout;
	private Integer connectionPoolSize;

	private String readConsistencyLevel;
	private String writeConsistencyLevel;

	public TitanGraph openGraph() {
		// remote API connection
		Configuration conf = new BaseConfiguration();
		conf.setProperty("storage.backend", backend);
		conf.setProperty("storage.hostname", hostname);
    conf.setProperty("storage.replication-factor", srf);
		if (autotype != null) {
			conf.setProperty("autotype", autotype);
		}
		logger.info(".....open graph for (" + backend + ", " + hostname + (autotype == null ? "" : ", " + autotype) + 
		    ", rf=" + srf + ")");

		if (connectionTimeout != null) {
			conf.setProperty("storage.connection-timeout", connectionTimeout);
			logger.info(".....with (connection-timeout: " + connectionTimeout + ")");
		}
		if (connectionPoolSize != null) {
			conf.setProperty("storage.connection-pool-size", connectionPoolSize);
			logger.info(".....with (connection-pool-size:" + connectionPoolSize + ")");
		}
		
		if (readConsistencyLevel != null && !"".equals(readConsistencyLevel.trim())) {
			conf.setProperty("storage.read-consistency-level", readConsistencyLevel);
			logger.info(".....with (read-consistency-level: " + readConsistencyLevel + ")");
		}
		if (writeConsistencyLevel != null && !"".equals(writeConsistencyLevel.trim())) {
			conf.setProperty("storage.write-consistency-level", writeConsistencyLevel);
			logger.info(".....with (write-consistency-level: " + writeConsistencyLevel + ")");
		}

		return initGraph(conf);
	}

	public TitanGraph openInMemoryGraph() {
		Configuration conf = new BaseConfiguration();
		conf.setProperty("storage.backend", "inmemory");
		// conf.setProperty("titan-version", "0.3.0");
		logger.info(".....open inmemory graph");
		return initGraph(conf);
	}

	private TitanGraph initGraph(Configuration conf) {
		TitanGraph graph = TitanFactory.open(conf);
		new GraphLoader(graph).init();
		return graph;
	}

	public String getBackend() {
		return backend;
	}

	public void setBackend(String backend) {
		this.backend = backend;
	}

	public String getHostname() {
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public String getAutotype() {
		return autotype;
	}

  public void setAutotype(String autotype) {
    this.autotype = autotype;
  }

  public String getSrf() {
	  return srf;
	}

  public void setSrf(String srf) {
    this.srf = srf;
  }
  
	public Integer getConnectionTimeout() {
		return connectionTimeout;
	}

	public void setConnectionTimeout(Integer connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}

	public Integer getConnectionPoolSize() {
		return connectionPoolSize;
	}

	public void setConnectionPoolSize(Integer connectionPoolSize) {
		this.connectionPoolSize = connectionPoolSize;
	}

	public String getReadConsistencyLevel() {
		return readConsistencyLevel;
	}

	public void setReadConsistencyLevel(String readConsistencyLevel) {
		this.readConsistencyLevel = readConsistencyLevel;
	}

	public String getWriteConsistencyLevel() {
		return writeConsistencyLevel;
	}

	public void setWriteConsistencyLevel(String writeConsistencyLevel) {
		this.writeConsistencyLevel = writeConsistencyLevel;
	}  
}
