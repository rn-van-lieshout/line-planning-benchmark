package util;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class DateExtender
{
	public static DateExtender createSevenWeeks()
	{
		return new DateExtender(LocalDate.parse("2021-10-04"), LocalDate.parse("2021-11-21"));
	}

	public static DateExtender create(LocalDate block1StartDate, LocalDate block1EndDate)
	{
		return new DateExtender(block1StartDate, block1EndDate);
	}

	private final LocalDate block1StartDate;
	private final LocalDate block1EndDate;
	private final int blockLength;

	private DateExtender(LocalDate block1StartDate, LocalDate block1EndDate)
	{
		if (block1EndDate.isBefore(block1StartDate))
		{
			throw new IllegalArgumentException();
		}
		this.block1StartDate = block1StartDate;
		this.block1EndDate = block1EndDate;
		this.blockLength = 1 + (int) ChronoUnit.DAYS.between(block1StartDate, block1EndDate);
	}

	public final boolean isInBlock1(LocalDate date)
	{
		return !this.block1StartDate.isAfter(date) && !this.block1EndDate.isBefore(date);
	}

	public final boolean isValidDate(LocalDate date)
	{
		return !this.block1StartDate.isAfter(date);
	}

	public final LocalDate getReferenceDate(LocalDate date)
	{
		if (!this.isValidDate(date))
		{
			throw new IllegalArgumentException(date.toString());
		}
		if (this.isInBlock1(date))
		{
			return date;
		}
		long delta = (ChronoUnit.DAYS.between(this.block1StartDate, date)) % this.blockLength;
		return this.block1StartDate.plusDays(delta);
	}
}