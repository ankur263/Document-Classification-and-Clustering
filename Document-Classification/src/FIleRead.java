import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;


public class FIleRead {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader("test.txt"));
		String s = null;
		int i = 0;
		while((s=br.readLine())!=null){
			String str[] = s.split(",");
			FileWriter fw = new FileWriter("dataset/"+i);
			fw.write(str[0]);
			fw.write("\n");
			fw.write(str[1]);
			fw.close();
			i++;
		}
		br.close();

	}

}
