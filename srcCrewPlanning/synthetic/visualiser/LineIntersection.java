package synthetic.visualiser;

public class LineIntersection
{
	// Point class for clarity
	public static class Point
	{
		double x, y;

		public Point(double x, double y)
		{
			this.x = x;
			this.y = y;
		}
	}

	// Function to find the orientation of ordered triplet (p, q, r)
	static double orientation(Point p, Point q, Point r)
	{
		double val = (q.y - p.y) * (r.x - q.x) -
				(q.x - p.x) * (r.y - q.y);

		if (val == 0) return 0; // colinear
		return (val > 0) ? 1 : 2; // clock or counterclock wise
	}

	// Check if point q lies on segment pr
	static boolean onSegment(Point p, Point q, Point r)
	{
		return q.x <= Math.max(p.x, r.x) && q.x >= Math.min(p.x, r.x) &&
				q.y <= Math.max(p.y, r.y) && q.y >= Math.min(p.y, r.y);
	}

	public static boolean doIntersect(Point p1, Point q1, Point p2, Point q2)
	{
		double o1 = orientation(p1, q1, p2);
		double o2 = orientation(p1, q1, q2);
		double o3 = orientation(p2, q2, p1);
		double o4 = orientation(p2, q2, q1);

		// General case only: lines intersect properly (not just touch)
		if (o1 != o2 && o3 != o4)
		{
			// Make sure the intersection is not at the endpoints
			if (!isEndpointIntersection(p1, q1, p2, q2))
			{
				return true;
			}
		}

		return false;
	}

	// Check if any of the endpoints are touching
	static boolean isEndpointIntersection(Point p1, Point q1, Point p2, Point q2)
	{
		return isSamePoint(p1, p2) || isSamePoint(p1, q2) ||
				isSamePoint(q1, p2) || isSamePoint(q1, q2);
	}

	// Utility to compare points
	static boolean isSamePoint(Point a, Point b)
	{
		return a.x == b.x && a.y == b.y;
	}
}
