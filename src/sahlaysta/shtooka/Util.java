package sahlaysta.shtooka;

//lazy private util class
final class Util {
	
	//test equal strings for both hashcode and content
	static boolean stringsEqual(String s1, String s2) {
		return
			s1 == s2
				? true
				: s1 == null || s2 == null
					? false
					: s1.hashCode() == s2.hashCode() && s1.equals(s2);
	}
	
}