using System;

namespace EventPlanner.Models
{
    public class Organizer : ICloneable
    {
        public string Name { get; set; }
        public string Phone { get; set; }
        public string Email { get; set; }
        public string Company { get; set; }

        public Organizer() { }

        // Конструктор копирования
        public Organizer(Organizer source)
        {
            Name = source.Name;
            Phone = source.Phone;
            Email = source.Email;
            Company = source.Company;
        }

        public object Clone()
        {
            return new Organizer(this);
        }

        public override string ToString()
        {
            return $"{Name} ({Company})";
        }
    }
}