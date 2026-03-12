using System;
using System.Collections.Generic;
using EventPlanner.Models;

namespace EventPlanner.Services
{
    public class TemplateRepository : ITemplateRepository
    {
        private List<EventTemplate> _templates = new List<EventTemplate>();

        public TemplateRepository()
        {
            LoadSampleData();
        }

        private void LoadSampleData()
        {
            _templates.Add(new EventTemplate
            {
                Name = "День рождения",
                Theme = "Вечеринка",
                Duration = TimeSpan.FromHours(4),
                ExpectedGuests = 20,
                Budget = 500,
                ColorCode = "#FF6B6B",
                RequiredItems = new List<string> { "Кейтеринг", "Музыка", "Декор", "Подарки" }
            });

            _templates.Add(new EventTemplate
            {
                Name = "Конференция",
                Theme = "Бизнес",
                Duration = TimeSpan.FromHours(8),
                ExpectedGuests = 100,
                Budget = 5000,
                ColorCode = "#4ECDC4",
                RequiredItems = new List<string> { "Кейтеринг", "Фотограф", "Ведущий", "Приглашения" }
            });

            _templates.Add(new EventTemplate
            {
                Name = "Свадьба",
                Theme = "Романтика",
                Duration = TimeSpan.FromHours(6),
                ExpectedGuests = 50,
                Budget = 3000,
                ColorCode = "#FFB6C1",
                RequiredItems = new List<string> { "Кейтеринг", "Музыка", "Декор", "Фотограф", "Транспорт" }
            });
        }

        public List<EventTemplate> GetAll() => _templates;

        public void Add(EventTemplate template) => _templates.Add(template);

        public void Update(EventTemplate oldTemplate, EventTemplate newTemplate)
        {
            int index = _templates.IndexOf(oldTemplate);
            if (index >= 0) _templates[index] = newTemplate;
        }

        public void Delete(EventTemplate template) => _templates.Remove(template);

        public EventTemplate CreateEventFromTemplate(EventTemplate template, string eventName)
        {
            return new EventTemplate
            {
                Name = eventName,
                Theme = template.Theme,     // < no prototype
                Duration = template.Duration,
                ExpectedGuests = template.ExpectedGuests,
                Budget = template.Budget,
                ColorCode = template.ColorCode,
                RequiredItems = new List<string>(template.RequiredItems)
            };
        }
    }
}