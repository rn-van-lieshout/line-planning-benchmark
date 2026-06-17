package operationalScheduling.sweetAndSour;

import operationalScheduling.sweetAndSour.attributes.Attribute;
import util.Configuration;

public class LinearScoringFunction extends AbstractScoringFunction
{
	private final double alpha;
	private final double margin;
	private final double alphaReduction;

	public LinearScoringFunction()
	{
		this.alpha = Configuration.getConfiguration().getDoubleProperty("ALPHA");
		this.margin = Configuration.getConfiguration().getDoubleProperty("MARGIN");
		this.alphaReduction = Configuration.getConfiguration().getDoubleProperty("ALPHA_REDUCTION_AT_MARGIN");
	}

	public double getPenalty(Attribute attribute, double score)
	{
		// Compute a non-negative normalised penalty when the current attribute score
		// deviates from the allowed attribute range.
		boolean isUpperBound = attribute.isUpperBound();
		double bound = attribute.getBound();
		double marginBound = attribute.getBound() + margin * bound * (isUpperBound ? -1 : 1);

		if (isUpperBound ? score <= marginBound : score >= marginBound)
		{
			// The score satisfies the margin bound.
			return 0;
		}
		if (isUpperBound ? score <= bound : score >= bound)
		{
			// The score satisfies the bound, but not the margin bound. Apply a reduced
			// penalty.
			return alpha * alphaReduction * (score - marginBound) * (isUpperBound ? 1 : -1) / bound;
		}
		else
		{
			// The score does not satisfy the bound. Apply a regular and reduced penalty.
			double reducedPenalty = alpha * alphaReduction * (bound - marginBound) * (isUpperBound ? 1 : -1) / bound;
			double regularPenalty = alpha * (score - bound) * (isUpperBound ? 1 : -1) / bound;
			return reducedPenalty + regularPenalty;
		}
	}
}
