using System;
using System.Collections.Generic;

namespace EventPlanner.Models
{
    public class EventTemplate : ICloneable
    {
        public string Name { get; set; }
        public string Theme { get; set; }
        public TimeSpan Duration { get; set; }
        public int ExpectedGuests { get; set; }
        public decimal Budget { get; set; }
        public List<string> RequiredItems { get; set; }
        public string ColorCode { get; set; }

        public Address Location { get; set; }
        public Organizer MainOrganizer { get; set; }

        public EventTemplate()
        {
            RequiredItems = new List<string>();
            ColorCode = "#FF6B6B";
            Location = new Address();
            MainOrganizer = new Organizer();
        }

        // КОНСТРУКТОР КОПИРОВАНИЯ - ядро паттерна Prototype
        public EventTemplate(EventTemplate source)
        {
            Name = source.Name;
            Theme = source.Theme;
            Duration = source.Duration;
            ExpectedGuests = source.ExpectedGuests;
            Budget = source.Budget;
            ColorCode = source.ColorCode;
            RequiredItems = new List<string>(source.RequiredItems);

            if (source.Location != null)
                Location = new Address(source.Location);

            if (source.MainOrganizer != null)
                MainOrganizer = new Organizer(source.MainOrganizer);
        }

        // Реализация ICloneable
        public object Clone()
        {
            return new EventTemplate(this);
        }
    }
}