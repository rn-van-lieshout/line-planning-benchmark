package operationalScheduling.sweetAndSour.attributes;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class AttributeVectorStorage
{
	private static AttributeVectorStorage attributeVectorStorage;

	public static AttributeVectorStorage getAttributeVectorStorage()
	{
		return (attributeVectorStorage == null)
				? attributeVectorStorage = new AttributeVectorStorage()
				: attributeVectorStorage;
	}
	
	public static void resetStorage()
	{
		attributeVectorStorage = new AttributeVectorStorage();
	}

	private final Map<Attribute, Map<Integer, AttributeVector>> attributeVectorMap;

	private AttributeVectorStorage()
	{
		this.attributeVectorMap = new LinkedHashMap<>();
	}

	public void addAttribute(Attribute attribute)
	{
		attributeVectorMap.put(attribute, new LinkedHashMap<>());
	}

	public AttributeVector getAttributeVector(Attribute attribute, int crewID)
	{
		if (!attributeVectorMap.get(attribute).containsKey(crewID))
		{
			attributeVectorMap.get(attribute).put(crewID, new AttributeVector(attribute.getAttributeVectorLength()));
		}
		return attributeVectorMap.get(attribute).get(crewID);
	}

	public Collection<Attribute> getAttributes()
	{
		return Collections.unmodifiableCollection(attributeVectorMap.keySet());
	}
}
