package nl.tudelft.aidm.optimalgroups.model;

import java.util.Objects;

public interface GroupSizeConstraint
{
	int minSize();
	int maxSize();

	String toString();

	static GroupSizeConstraint manual(int min, int max)
	{
		return new Manual(min, max);
	}
	
	record Manual(int minSize, int maxSize) implements GroupSizeConstraint
	{
		@Override
		public String toString()
		{
			return String.format("GSC[%s,%s]", minSize(), maxSize());
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;

			if (o instanceof GroupSizeConstraint gsc) {
				return this.minSize == gsc.minSize() && this.maxSize == gsc.maxSize();
			}

			return false;
		}

		@Override
		public int hashCode() {
			return Objects.hash(minSize, maxSize);
		}
	}
}
