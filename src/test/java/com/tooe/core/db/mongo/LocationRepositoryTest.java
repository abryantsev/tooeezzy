package com.tooe.core.db.mongo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import scala.Option;
import scala.None$;
import scala.Some;

import com.tooe.core.db.mongo.domain.Location;
import com.tooe.core.db.mongo.repository.LocationRepository;

@Ignore
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath:META-INF/spring/test/spring-data-mongo-ctxt.xml"})
public class LocationRepositoryTest  {

	@Autowired
	protected LocationRepository locationRepo;

	@Test
	public void testLocationRepository() {
		System.out.println("dbfield: "+dbfield("presentsCount"));
		System.out.println("locationRepo: "+locationRepo.toString());
		ObjectId testLocationId = new ObjectId("51404195dc6ad5d6d3c0b0b5");
		ObjectId testRegionId = new ObjectId("51404195dc6ad5d6d3c0b0b6");
		String testCategoryId = "garden9dba43e2-77b9-4397-9720-65289979c30b";
		String testLocationName = "testProductName";
		String testLocationDescription = "testDescription";
		String testOpeningHours = "testOpeningHours";
		List<Location> locations = Collections.emptyList(); //TODO comment up not compilable code, test won't pass anyway locationRepo.searchLocationsBy(testRegionId, testCategoryId, testLocationName, true, option(), new PageRequest(0,10));
		//String fields = dbfields("name");
		//List<Location> locations = locationRepo.searchLocationsBy(testRegionId, testCategoryId, testLocationName, true, option(), new PageRequest(0,10), Arrays.asList("n", "oh"));
		System.out.println("locations: "+locations.toString());
		Location location = locations.get(0);
		System.out.println("location.name: "+location.name());
		Assert.assertNotNull(locations);
	}

  public static <T> Option<T> option() {
    return (Option<T>) (new Some<Boolean>(true));
  }
	
  public static String dbfields(String... fields) {
  	StringBuffer sb = new StringBuffer();
  	int i = 0;
  	for(String field : fields){
  		if(i > 0){
    		sb.append(",");
  		}
  		sb.append(dbfield(field));
  		sb.append(": 1");
  		i++;
  	}
  	return sb.toString();
  }

  public static String dbfield(String field) {
  	String fn = "";
  	try {
  		fn = Location.class.getDeclaredField(field).getAnnotation(org.springframework.data.mongodb.core.mapping.Field.class).value();
		} catch (NoSuchFieldException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
  	return fn;
  }
}
