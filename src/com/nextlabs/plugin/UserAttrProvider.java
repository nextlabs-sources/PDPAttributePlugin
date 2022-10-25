/**
 * 
 */
package com.nextlabs.plugin;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Properties;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bluejungle.framework.expressions.EvalValue;
import com.bluejungle.framework.expressions.IEvalValue;
import com.bluejungle.framework.expressions.Multivalue;
import com.bluejungle.pf.domain.destiny.serviceprovider.ISubjectAttributeProvider;
import com.bluejungle.pf.domain.destiny.serviceprovider.ServiceProviderException;
import com.bluejungle.pf.domain.destiny.subject.IDSubject;
import com.bluejungle.pf.domain.destiny.subject.Subject;
import com.nextlabs.plugin.helper.ACConstants;
import com.nextlabs.plugin.helper.PropertyLoader;
/**
 * @author klee
 * 
 */
public class UserAttrProvider implements ISubjectAttributeProvider {

	private static final Log LOG = LogFactory
			.getLog(UserAttrProvider.class);
	private static Properties prop = PropertyLoader
			.loadPropertiesInPDP(ACConstants.COMMON_UAP_PROPFILE);	

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.bluejungle.pf.domain.destiny.serviceprovider.IServiceProvider#init()
	 */
	@Override
	public void init() throws Exception {
	
		LOG.info("ResourceAttrProvider init() completed.");
	}

	
	@Override
	public IEvalValue getAttribute(IDSubject arg0, String arg1) throws ServiceProviderException {
		IEvalValue value = IEvalValue.NULL;
		String userId = (String) arg0.getUid();
		String attributeName = arg1;
		if (LOG.isDebugEnabled())
			LOG.info("UAP identifier:" + userId);

		LOG.info("UAP getAttribute " + attributeName);

		if (userId.length() > 0 && attributeName.length() > 0) {
			
			Reader csvData = null;
			CSVParser parser = null;
			
			try {

				LOG.info("CSV file location " + prop.getProperty("csv_file_location"));

				if (prop.getProperty("csv_file_location") == null) {
					LOG.info("Error loading csv file at" + prop.getProperty("csv_file_location"));
					return value;
				}

				csvData = new FileReader(prop.getProperty("csv_file_location"));

				parser = CSVParser.parse(csvData, CSVFormat.RFC4180);
				
				boolean found = false;

				for (CSVRecord csvRecord : parser) {

					if (csvRecord.get(0).equalsIgnoreCase(userId)) {

						int iColumn = Integer.parseInt(prop.getProperty(attributeName + "_column", "0"));
						
						found = true;

						if (iColumn > 0) {

							String sData = csvRecord.get(iColumn);

							LOG.info("Found VALUE: " + sData);

							if (sData == null || sData.equals("") || sData.length() < 1) {
								LOG.info("Return IEValve NULL for empty string");
								return IEvalValue.NULL;
							}

							String[] sDataArr = sData.split("\\|");
							

							if (sDataArr.length < 2) {
								return (EvalValue.build(sData));
							} else {
								ArrayList<String> result = new ArrayList<String>();

								LOG.info("Multi Value data found for attribute " + attributeName);
								
								for (int i=0; i<sDataArr.length; i++) {
									result.add(sDataArr[i]);
									LOG.info("Adding value "+ sDataArr[i] +" to the list");
									
								}
																
								return EvalValue.build(Multivalue.create(result));
							}

						} else {
							LOG.warn("Column is not defined for " + attributeName +" returning java NULL");
							return null;
						}

					}
				}
				
				if (!found) {
					LOG.error("No record found for user "+ userId +", will return IEVal NULL");
				}

			} catch (IOException e) {
				LOG.error(e);
			}
			finally {
				if (parser != null)
					try {
						parser.close();
					} catch (IOException e) {
					}
				if (csvData != null)
					try {
						csvData.close();
					} catch (IOException e) {
					}
			}

		}
		return value;
	}
	
	
	public static void main(String args[]) {
		
		UserAttrProvider uap = new UserAttrProvider();
		
		try {
			uap.init();
			
			Subject sbj =new Subject("USER1",null);
		
			System.out.println(sbj.getUid());

			Thread.sleep(1000);
			System.out.println(uap.getAttribute(sbj, "citizenship"));
		} catch (ServiceProviderException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
