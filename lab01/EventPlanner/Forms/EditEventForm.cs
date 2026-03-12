using System;
using System.Drawing;
using System.Windows.Forms;
using EventPlanner.Models;

namespace EventPlanner.Forms
{
    public class EditEventForm : Form
    {
        private const int FORM_WIDTH = 450;
        private const int FORM_HEIGHT = 300;

        private TextBox _txtName;
        private NumericUpDown _nudGuests;
        private NumericUpDown _nudHours;
        private NumericUpDown _nudMinutes;
        private TextBox _txtBudget;
        private Button _btnSave;
        private Button _btnCancel;

        public EventTemplate Event { get; private set; }

        public EditEventForm(EventTemplate eventTemplate)
        {
            Event = new EventTemplate(eventTemplate);
            InitializeComponent();
            LoadEventData();
        }

        private void InitializeComponent()
        {
            this.Text = "Редактирование мероприятия";
            this.Size = new Size(FORM_WIDTH, FORM_HEIGHT);
            this.StartPosition = FormStartPosition.CenterParent;
            this.FormBorderStyle = FormBorderStyle.FixedDialog;
            this.MaximizeBox = false;
            this.MinimizeBox = false;

            int currentY = 20;
            int labelX = 20;
            int controlX = 120;
            int controlWidth = 250;

            this.Controls.Add(new Label { Text = "Название:", Location = new Point(labelX, currentY + 3), Width = 80 });
            _txtName = new TextBox { Location = new Point(controlX, currentY), Width = controlWidth };
            this.Controls.Add(_txtName);
            currentY += 30;

            this.Controls.Add(new Label { Text = "Гостей:", Location = new Point(labelX, currentY + 3), Width = 80 });
            _nudGuests = new NumericUpDown { Location = new Point(controlX, currentY), Width = 100, Minimum = 1, Maximum = 1000 };
            this.Controls.Add(_nudGuests);
            currentY += 30;

            this.Controls.Add(new Label { Text = "Длительность:", Location = new Point(labelX, currentY + 3), Width = 80 });

            _nudHours = new NumericUpDown { Location = new Point(controlX, currentY), Width = 60, Minimum = 0, Maximum = 24 };
            this.Controls.Add(_nudHours);
            this.Controls.Add(new Label { Text = "ч", Location = new Point(controlX + 65, currentY + 3), Width = 20 });

            _nudMinutes = new NumericUpDown { Location = new Point(controlX + 90, currentY), Width = 60, Minimum = 0, Maximum = 59 };
            this.Controls.Add(_nudMinutes);
            this.Controls.Add(new Label { Text = "мин", Location = new Point(controlX + 155, currentY + 3), Width = 40 });
            currentY += 30;

            this.Controls.Add(new Label { Text = "Бюджет ($):", Location = new Point(labelX, currentY + 3), Width = 80 });
            _txtBudget = new TextBox { Location = new Point(controlX, currentY), Width = 100 };
            this.Controls.Add(_txtBudget);
            currentY += 50;

            _btnSave = new Button
            {
                Text = "Сохранить",
                Location = new Point(120, currentY),
                Size = new Size(120, 35),
                BackColor = Color.FromArgb(52, 152, 219),
                ForeColor = Color.White,
                FlatStyle = FlatStyle.Flat
            };
            _btnSave.Click += BtnSave_Click;

            _btnCancel = new Button
            {
                Text = "Отмена",
                Location = new Point(250, currentY),
                Size = new Size(120, 35),
                BackColor = Color.FromArgb(231, 76, 60),
                ForeColor = Color.White,
                FlatStyle = FlatStyle.Flat
            };
            _btnCancel.Click += (s, e) => DialogResult = DialogResult.Cancel;

            this.Controls.AddRange(new Control[] { _btnSave, _btnCancel });
        }

        private void LoadEventData()
        {
            _txtName.Text = Event.Name;
            _nudGuests.Value = Event.ExpectedGuests;
            _nudHours.Value = Event.Duration.Hours;
            _nudMinutes.Value = Event.Duration.Minutes;
            _txtBudget.Text = Event.Budget.ToString("F2");
        }

        private void BtnSave_Click(object sender, EventArgs e)
        {
            if (string.IsNullOrWhiteSpace(_txtName.Text))
            {
                MessageBox.Show("Введите название мероприятия", "Ошибка",
                    MessageBoxButtons.OK, MessageBoxIcon.Warning);
                return;
            }

            Event.Name = _txtName.Text;
            Event.ExpectedGuests = (int)_nudGuests.Value;
            Event.Duration = new TimeSpan((int)_nudHours.Value, (int)_nudMinutes.Value, 0);

            if (decimal.TryParse(_txtBudget.Text, out decimal budget))
                Event.Budget = budget;

            DialogResult = DialogResult.OK;
        }
    }
}