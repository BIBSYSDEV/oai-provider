package no.sikt.oai.temp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;

public class OAIConfig {

	/*
	 * Definisjon av data sett, består av setSpec - ID for settet setNavn - er
	 * litt mer høynivå navn for settet, foreløpig det samme som setSpec
	 * database - hvilken database søker vi i;  biblio, brage eller authority  
 	 * query - CQL som gir utvalget av poster i settet
	 * 
	 * OBS! OBS! OBS! Sett 'default' er settet som blir brukt hvis setSpec
	 * ikke er gitt !!!! 
	 *  
	 */
	transient static final Logger LOG = LoggerFactory.getLogger(OAIConfig.class);

	private int MAXIMUM_REQUEST_RETRY = 1;
	private int PAGE_SIZE = 50;
	private int MAXIMUM_CONCURRENT_REQUESTS = 1;
	private int REQUEST_RETRY_AFTER = 300;
	private final String DATE_GRANULARITY = "YYYY-MM-DDThh:mm:ssZ";
    private final String EARLIEST_TIMESTAMP = "1976-01-01T00:00:01Z";
    private final String DELETED_RECORD = "yes";
    private final String REPOSITORY_NAME = "BIBSYS Library Automation System Repository";
    private final String PROTOCOL_VERSION = "2.0";
    private final String ADMIN_EMAIL = "bibdrift@bibsys.no";
	private Hashtable<String, OAIDatasetDefinition> dataSets;
	private String applicationId;
	private String[] metadataFormats;
	private String[] setNames;

	private String configFilename;
	private static OAIConfig oaiConfig;
	
	public OAIConfig(String configFilename) {
		this.configFilename = configFilename;
		initier();
	}

	public static final OAIConfig getInstance(String applicationId, String configFilename) {
		if (oaiConfig != null) {
			return oaiConfig;
		}
		return new OAIConfig(configFilename);
	}
	
	public final void initier() {
		dataSets = new Hashtable<>();
		LOG.info(applicationId, "OAIConfig.init() laster fra " + configFilename);
		
		if (configFilename == null) {
			return;
		} else {
			try {
				loadDatasetConfigurationInst(configFilename);
			} catch (FileNotFoundException e) {
				LOG.error(applicationId, "Finner ikke konfig-fil: " + configFilename);
			} catch (IOException e) {
				LOG.error(applicationId, "IOException ved lesing av konfig-fil: " + configFilename);
			}
	        System.out.println("OAIConfig.init() lastet fra " + configFilename);			
		}
    }


	static String expandProperty(String myKey,Properties myProperties) {
		
		String START_CONST = "${";
		String END_CONST = "}";
			
		String myValue = myProperties.getProperty(myKey);
		
		int beginIndex = 0;
		int startName = myValue.indexOf(START_CONST, beginIndex);

		while (startName != -1) {

			int endName = myValue.indexOf(END_CONST, startName);
			if (endName == -1) {
				// Terminating symbol not found, Return the value as is
				break;
			}

			String constName = myValue.substring(startName + 2, endName);
			String constValue = myProperties.getProperty(constName);

			if (constValue == null) {
				// Property name not found, Return the value as is
				break;
			}

			// Insert the constant value into the original property value
			String newValue = (startName > 0) ? myValue.substring(0, startName) : "";
			newValue += constValue;

			// Start checking for constants at this index
			beginIndex = newValue.length();

			// Append the remainder of the value
			newValue += myValue.substring(endName + 1);
			myValue = newValue;

			// Look for the next constant
			startName = myValue.indexOf(START_CONST, beginIndex);
		}	
		return myValue;
	}

	private void loadDatasetConfigurationInst(String fileName) throws FileNotFoundException, IOException {

		LOG.debug(applicationId, "OAIConfig - laster datasetdefinisjoner fra " + fileName);

		Properties oaiProperties = new Properties();
		oaiProperties.load(new FileInputStream(fileName));
		
		/*
		 *  Iterate and expand properties that refer to other properties like:
		 *  nora_hia_vitenskaplig=nora_hia_vitenskaplig,brage,inst=hia and ${nora_dc_type_cql}
		 */
		@SuppressWarnings("unchecked")
		Enumeration<String> el = (Enumeration<String>) oaiProperties.propertyNames();		
		while (el.hasMoreElements()) {
			String myKey = el.nextElement();								
			String myValue = expandProperty(myKey, oaiProperties);
			oaiProperties.setProperty(myKey, myValue);
		}

		String maxProcesses = oaiProperties.getProperty("maximum_concurrent_requests");
		if (maxProcesses != null && maxProcesses.length() > 0) {
			try {
                setMaximumConcurrentRequests(Integer.parseInt(maxProcesses));
			} catch (NumberFormatException e) {
                System.out.println(e);
                LOG.error(applicationId, "NumberFormatException ved lesing av maximum_concurrent_requests: " + maxProcesses + ", bruker default "
						+ getMaximumConcurrentRequests());
			}
		}

		String retryAfterStr = oaiProperties.getProperty("request_retry_after");
		if (retryAfterStr != null && retryAfterStr.length() > 0) {
			try {
                setRequestRetryAfter(Integer.parseInt(retryAfterStr));
			} catch (NumberFormatException e) {
				LOG.error(applicationId, "NumberFormatException ved lesing av request_retry_after: " + retryAfterStr + ", bruker default " + getRequestRetryAfter());
			}
		}

		String pageSize = oaiProperties.getProperty("page_size");
		if (pageSize != null && pageSize.length() > 0) {
			try {
                setPageSize(Integer.parseInt(pageSize));
			} catch (NumberFormatException e) {
				LOG.error(applicationId, "NumberFormatException ved lesing av pageSize: " + pageSize + ", bruker default " + getPageSize());
			}
		}
		
		String maximum_request_retryString = oaiProperties.getProperty("maximum_request_retry");
		if (maximum_request_retryString != null && maximum_request_retryString.length() > 0) {
			try {
                setMaximumRequestRetry(Integer.parseInt(maximum_request_retryString));
			} catch (NumberFormatException e) {
				LOG.error(applicationId, "NumberFormatException ved lesing av maximum_request_retry: " + MAXIMUM_REQUEST_RETRY + ", bruker default " + getMaximumRequestRetry());
			}
		}

		String mdf = oaiProperties.getProperty("metadataPrefixes");
		metadataFormats = mdf.split(",");


		String _setNames = oaiProperties.getProperty("setSpecs");
		setNames = _setNames.split(",");

		for (int i = 0; i < setNames.length; i++) {
			String id = setNames[i].trim();
			String setDef = oaiProperties.getProperty(id);
			if (setDef != null) {
                String[] setDetails = setDef.split(",");
                String settNavn = setDetails[0].trim();
                String solrQuery = "";
                if (setDetails.length > 1) {
                    solrQuery = " AND " + setDetails[1].trim();
                }
				OAIDatasetDefinition oaSetDef = new OAIDatasetDefinition(id, settNavn, solrQuery);
				dataSets.put(id, oaSetDef);
			}
		}

	}

	public String[] getMetadataFormats() {
		return metadataFormats;
	}

	public Collection<OAIDatasetDefinition> getDatasets() {
		return dataSets.values();

	}

	public OAIDatasetDefinition getDataset(String setSpec) {
		if (setSpec == null || setSpec.trim().isEmpty()) {
			setSpec = "default";
		}
		if (dataSets.containsKey(setSpec)) {
			return dataSets.get(setSpec);
		} else {
			return null;
		}
	}

	public boolean isValidSetName(String s) {
		return dataSets.containsKey(s);
	}


	public String dumpInnhold() {
		StringBuilder buf = new StringBuilder();
		
		buf.append("\n"+new Date(System.currentTimeMillis()).toString()+"\n\n");

		buf.append("Gjeldende konfigFil: " + configFilename);
		buf.append("\n\n");
		buf.append("Max antall samtidige prosesser (maximum_concurrent_requests): " + getMaximumConcurrentRequests() + "\n\n");
		buf.append("Antall poster som returneres (page_size): " + getPageSize() + "\n\n");
//TODO
//		buf.append("Antall kjørende prosesser (CURRENT_REQUEST_COUNT): " + OAIRepositoryServlet.getCurrentRequestCount() + "\n\n");
		buf.append("Ventetid ved opptatt server (request_retry_after): " + getRequestRetryAfter() + "\n\n");
		buf.append("Timeout ved søk, fra BIFROST (CONCURRENT_SEARCH_TIMEOUT_IN_SECONDS): " + getSearchTimeoutInSeconds() + "\n\n");		
		buf.append("Max antall forsøk ved timeout (maximum_request_retry): " + getMaximumRequestRetry() + "\n\n");		
		buf.append("Kjente metadataprefix: ");
		for (int i = 0; i < metadataFormats.length; i++) {
			buf.append(metadataFormats[i] + " ");
		}
		buf.append("\n \n");
		
		buf.append("Mapping av metadataprefix, builder og presentasjon:\n");

		buf.append("\n \n");
		
		Collection<OAIDatasetDefinition> c = getDatasets();
		Iterator<OAIDatasetDefinition> e = c.iterator();
		while (e.hasNext()) {
			OAIDatasetDefinition setDef =  e.next();
			if (!setDef.getSetSpec().equalsIgnoreCase("default")) {
				buf.append("setSpec: " + setDef.getSetSpec() + "\n");
				buf.append("setName: " + setDef.getSettNavn() + "\n");
				buf.append("Query: " + setDef.getQueryString() + "\n");
				buf.append("Antall poster: " + setDef.getAntallPoster() + "\n");
				buf.append("Sist kjørt: ");
				if (setDef.getTidspunktSisteInnhosting() > 0) {
					buf.append(new Date(setDef.getTidspunktSisteInnhosting()).toString());
				} else {
					buf.append(" -- Ikke kjørt etter nullstilling av buffer --");
				}

			} else {
				buf.append("defaultset - innhøsting fra hele basen\n");
				buf.append("Query: " + setDef.getQueryString() + "\n");
				buf.append("Antall poster: " + setDef.getAntallPoster() + "\n");
				buf.append("Sist kjørt: ");
				if (setDef.getTidspunktSisteInnhosting() > 0) {
					buf.append(new Date(setDef.getTidspunktSisteInnhosting()).toString());
				} else {
					buf.append(" -- Ikke kjørt etter nullstilling av buffer --");
				}

			}
			buf.append(" \n\n");
		}

		return buf.toString();
	}

	private String getSearchTimeoutInSeconds() {
		// TODO Auto-generated method stub
		return null;
	}


	public void updateTimeStamp(String setSpec, long timeStamp) {
		if (setSpec == null || setSpec.length() == 0) {
			setSpec = "default";
		}
		if (setSpec != null && dataSets.containsKey(setSpec)) {
			OAIDatasetDefinition setDef = dataSets.get(setSpec);
			setDef.setTidspunktSisteInnhosting(timeStamp);
		}
	}

	public void updateRecordCount(String setSpec, long recordCount) {
		if (setSpec != null && dataSets.containsKey(setSpec)) {
			OAIDatasetDefinition setDef = dataSets.get(setSpec);
			if (setDef.getAntallPoster() <= 0) {
				setDef.setAntallPoster(recordCount);
			}
		}
	}

	public String getApplicationId() {
		return applicationId;
	}

	public void setApplicationId(String applicationId) {
		this.applicationId = applicationId;
	}

	public void setMaximumRequestRetry(int MAXIMUM_REQUEST_RETRY) {
        this.MAXIMUM_REQUEST_RETRY = MAXIMUM_REQUEST_RETRY;
	}

	public int getMaximumRequestRetry() {
		return MAXIMUM_REQUEST_RETRY;
	}

	public String getDateGranularity() {
		return DATE_GRANULARITY;
	}

    public String getRepositoryName() {
        return REPOSITORY_NAME;
    }

    public String getProtocolVersion() {
        return PROTOCOL_VERSION;
    }

    public String getAdminEmail() {
        return ADMIN_EMAIL;
    }

    public String getDeletedRecord() {
        return DELETED_RECORD;
    }

    public String getEarliestTimestamp() {
        return EARLIEST_TIMESTAMP;
    }

    public void setMaximumConcurrentRequests(
			int MAXIMUM_CONCURRENT_REQUESTS) {
        this.MAXIMUM_CONCURRENT_REQUESTS = MAXIMUM_CONCURRENT_REQUESTS;
	}


	public int getMaximumConcurrentRequests() {
		return MAXIMUM_CONCURRENT_REQUESTS;
	}


	public void setRequestRetryAfter(int REQUEST_RETRY_AFTER) {
        this.REQUEST_RETRY_AFTER = REQUEST_RETRY_AFTER;
	}


	public int getRequestRetryAfter() {
		return REQUEST_RETRY_AFTER;
	}


	public void setPageSize(int PAGE_SIZE) {
        this.PAGE_SIZE = PAGE_SIZE;
	}


	public int getPageSize() {
		return PAGE_SIZE;
	}
}
