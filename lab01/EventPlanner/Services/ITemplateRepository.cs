using System.Collections.Generic;
using EventPlanner.Models;

namespace EventPlanner.Services
{
    public interface ITemplateRepository
    {
        List<EventTemplate> GetAll();
        void Add(EventTemplate template);
        void Update(EventTemplate oldTemplate, EventTemplate newTemplate);
        void Delete(EventTemplate template);
        EventTemplate CreateEventFromTemplate(EventTemplate template, string eventName);
    }
}