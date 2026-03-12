using System;
using System.Collections.Generic;

namespace EventPlanner.Models
{
    public class EventTemplate
    {
        public string Name { get; set; }
        public string Theme { get; set; }
        public TimeSpan Duration { get; set; }
        public int ExpectedGuests { get; set; }
        public decimal Budget { get; set; }
        public List<string> RequiredItems { get; set; }
        public string ColorCode { get; set; }

        public EventTemplate()
        {
            RequiredItems = new List<string>();
            ColorCode = "#FF6B6B";
        }

        // Конструктор копирования для Prototype
        public EventTemplate(EventTemplate source)
        {
            Name = source.Name;
            Theme = source.Theme;
            Duration = source.Duration;
            ExpectedGuests = source.ExpectedGuests;
            Budget = source.Budget;
            ColorCode = source.ColorCode;
            RequiredItems = new List<string>(source.RequiredItems);
        }
    }
}