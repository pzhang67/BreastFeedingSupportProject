package gatech.hi.fhirpit.app.api;

import ca.uhn.fhir.model.dstu2.composite.AddressDt;
import ca.uhn.fhir.model.dstu2.composite.ContactPointDt;
import ca.uhn.fhir.model.dstu2.composite.HumanNameDt;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.model.dstu2.resource.Patient;
import gatech.hi.fhirpit.app.domain.RequestInfo;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ca.uhn.fhir.model.dstu2.resource.ReferralRequest;
import ca.uhn.fhir.model.dstu2.valueset.ReferralStatusEnum;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.IGenericClient;
import gatech.hi.fhirpit.app.config.FHIRconfig;
import gatech.hi.fhirpit.app.repo.RequestsRepo;

import java.util.ArrayList;
import java.util.List;
@RestController
public class UpdateReferralStatusApi {
      @Autowired
      RequestsRepo repo;
      
	  @Autowired
	  FHIRconfig config;
	
	  @RequestMapping(value = "/updateStatus/{id}", method = RequestMethod.PUT)
	    public ResponseEntity<String> updateUser(@PathVariable("id") String id, @RequestParam("status") String status) {
	        System.out.println("Updating Referal Request " + id + "status >>" + status);
	        IGenericClient client = config.getClient();
	        ReferralRequest req = repo.getReferralRequest(id);
	        System.out.println("Updating Referral Request >>" + req);
	        
	        if(req !=null)
	        {
	        	
	        	
	        	req.setStatus(getStatus(status));            	
               	MethodOutcome res = client.update().resource(req).execute();
               	res.getOperationOutcome();
	        	
	        }
	         
	        MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
	        params.set("status", "done");
	     
	       return new ResponseEntity<String>(params,HttpStatus.OK);
	    }


	@RequestMapping(value = "/getAllReferral",  method = RequestMethod.GET)
	public ResponseEntity<List<RequestInfo>> getAllReferral() {

	  	List<RequestInfo> list = new ArrayList<RequestInfo>();
		System.out.println("Getting all referral requests !!!");
		IGenericClient client = config.getClient();

		Bundle refferals = client
				.search()
				.forResource(ReferralRequest.class)
				.returnBundle(ca.uhn.fhir.model.dstu2.resource.Bundle.class)
				.execute();

		for(Bundle.Entry en : refferals.getEntry())
		{
			ReferralRequest r = (ReferralRequest) en.getResource();


			String patientId = r.getPatient().getReference().getValueAsString();
			patientId = StringUtils.substringAfter(patientId, "/");
			Bundle patients = client
					.search()
					.forResource(Patient.class)
					//.where(Patient.IDENTIFIER.exactly().code(patientId))
					.where(Patient.RES_ID.matches().value(patientId))
					.returnBundle(ca.uhn.fhir.model.dstu2.resource.Bundle.class)
					.execute();

			Patient patient = null;
			for(Bundle.Entry entr : patients.getEntry())
			{
				patient = (Patient) entr.getResource();
			}

			if(StringUtils.equals("active",r.getStatus()))
			{
				list.add(createReqInfo(r,patient));
			}
		}
		System.out.println("testing");
		System.out.println(list.size());
		return new ResponseEntity<List<RequestInfo>>(list,HttpStatus.OK);
	}


	@RequestMapping(value = "/getAllAcceptedReferral",  method = RequestMethod.GET)
	public ResponseEntity<List<RequestInfo>> getAllAcceptedReferral() {

		List<RequestInfo> list = new ArrayList<RequestInfo>();
		System.out.println("Getting all referral requests !!!");
		IGenericClient client = config.getClient();

		Bundle refferals = client
				.search()
				.forResource(ReferralRequest.class)
				.returnBundle(ca.uhn.fhir.model.dstu2.resource.Bundle.class)
				.execute();

		for(Bundle.Entry en : refferals.getEntry())
		{
			ReferralRequest r = (ReferralRequest) en.getResource();


			String patientId = r.getPatient().getReference().getValueAsString();
			patientId = StringUtils.substringAfter(patientId, "/");
			Bundle patients = client
					.search()
					.forResource(Patient.class)
					//.where(Patient.IDENTIFIER.exactly().code(patientId))
					.where(Patient.RES_ID.matches().value(patientId))
					.returnBundle(ca.uhn.fhir.model.dstu2.resource.Bundle.class)
					.execute();

			Patient patient = null;
			for(Bundle.Entry entr : patients.getEntry())
			{
				patient = (Patient) entr.getResource();
			}

			if(StringUtils.equals("accepted",r.getStatus()))
			{
				list.add(createReqInfo(r,patient));
			}
		}

		return new ResponseEntity<List<RequestInfo>>(list,HttpStatus.OK);
	}


	private ReferralStatusEnum getStatus(String status) {
	
		if(StringUtils.equals("accepted", status))
		{
		   return ReferralStatusEnum.ACCEPTED;	
		}
		if(StringUtils.equals("completed", status))
		{
			return ReferralStatusEnum.COMPLETED;
		}
		if(StringUtils.equals("rejected", status))
		{
			return ReferralStatusEnum.REJECTED;
		}
		
		return ReferralStatusEnum.ACCEPTED;	
	}


	private RequestInfo createReqInfo(ReferralRequest req, Patient patient) {




		RequestInfo requestInfo = new RequestInfo();
		requestInfo.setReferalId(req.getId().getIdPart());
		for(ResourceReferenceDt rf : req.getSupportingInformation())
		{
			requestInfo.setService(rf.getReference().getValue());
		}

		if(patient==null)
			return null;

		for(HumanNameDt name : patient.getName())
		{

			requestInfo.setName(name.getNameAsSingleString());
			System.out.println("name >>>>>" + name.getNameAsSingleString());
		}

		for(AddressDt add : patient.getAddress())
		{
			requestInfo.setAddress(add.getLine() + " " + add.getCity() + " " + add.getState() + " " + add.getCountry() + " " + add.getPostalCode());
			//System.out.println("adddress >>>>>" + add.getCity() + add.getState() + add.getCountry() + add.getLine() );
		}

		for(ContactPointDt ct : patient.getTelecom())
		{
			requestInfo.setContactInfo(ct.getSystem() + " : " + ct.getValue() + " \t");
			System.out.println("telecom >>>>>" + ct.getSystem() + ">>>" + ct.getValue());
		}

		requestInfo.setDob(patient.getBirthDate() + "");

		requestInfo.setGender(patient.getGender());


		System.out.println( patient.getBirthDate() + ">>>>" + patient.getMaritalStatus().getText());

		JSONObject j   = new JSONObject(requestInfo);

		//return j.toString();
		return requestInfo;
	}


}
