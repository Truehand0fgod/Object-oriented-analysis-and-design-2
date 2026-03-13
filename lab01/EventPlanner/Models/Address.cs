using System;

namespace EventPlanner.Models
{
    public class Address : ICloneable
    {
        public string City { get; set; }
        public string Street { get; set; }
        public string Building { get; set; }
        public string Venue { get; set; }

        public Address() { }

        public Address(Address source)
        {
            City = source.City;
            Street = source.Street;
            Building = source.Building;
            Venue = source.Venue;
        }

        public object Clone()
        {
            return new Address(this);
        }

        public override string ToString()
        {
            return $"{Venue} ({City}, {Street} {Building})";
        }
    }
}