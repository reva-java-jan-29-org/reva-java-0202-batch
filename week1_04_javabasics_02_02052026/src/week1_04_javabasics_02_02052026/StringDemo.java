package week1_04_javabasics_02_02052026;

public class StringDemo {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		String mobileNo = "8976343234";
		
		//should have 10 digits
		//all the characters should be digits
		//should start with 7 / 8 / 9
		
		
		boolean result =  mobileNo.matches("[789]\\d{9}");
		System.out.println(result);
		
	}

}
