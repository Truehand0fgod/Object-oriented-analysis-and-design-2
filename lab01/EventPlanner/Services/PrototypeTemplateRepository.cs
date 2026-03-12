using System;
using System.Collections.Generic;
using EventPlanner.Models;

namespace EventPlanner.Services
{
    public class PrototypeTemplateRepository : ITemplateRepository
    {
        private List<EventTemplate> _templates = new List<EventTemplate>();

        public PrototypeTemplateRepository()
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
                Budget = 200,
                ColorCode = "#FF6B6B",
                RequiredItems = new List<string> { "Кейтеринг", "Музыка", "Декор", "Подарки" }
            });

            _templates.Add(new EventTemplate
            {
                Name = "Конференция",
                Theme = "Бизнес",
                Duration = TimeSpan.FromHours(8),
                ExpectedGuests = 100,
                Budget = 2500,
                ColorCode = "#4ECDC4",
                RequiredItems = new List<string> { "Кейтеринг", "Фотограф", "Ведущий", "Приглашения" }
            });

            _templates.Add(new EventTemplate
            {
                Name = "Свадьба",
                Theme = "Романтика",
                Duration = TimeSpan.FromHours(6),
                ExpectedGuests = 50,
                Budget = 2000,
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
            var newEvent = new EventTemplate(template); // < Prototype!
            newEvent.Name = eventName;
            return newEvent;
        }
    }
}