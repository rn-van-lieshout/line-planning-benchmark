package data.read;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DayReader
{
	public static List<LocalDate> readDays(String file) throws IOException
	{
		List<LocalDate> days = new ArrayList<>();

		BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
		String line = bufferedReader.readLine();
		while (line != null)
		{
			days.add(LocalDate.parse(line));
			line = bufferedReader.readLine();
		}
		bufferedReader.close();
		return days;
	}
}
