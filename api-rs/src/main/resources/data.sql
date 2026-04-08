-- Sample Categories
INSERT INTO CATEGORY (name, code, description) VALUES
    ('Inbox',    'INBOX',    'General incoming messages'),
    ('Work',     'WORK',     'Work-related emails and notifications'),
    ('Personal', 'PERSONAL', 'Personal correspondence');

-- Sample Messages — INBOX
INSERT INTO MESSAGE (title, body, category_code) VALUES
    ('Welcome to Emails Summarizer',
     'Thank you for trying Emails Summarizer. This app helps you organize and summarize your emails efficiently.',
     'INBOX'),
    ('System Notification',
     'Your account has been successfully set up. You can now start organizing your emails by category.',
     'INBOX'),
    ('Newsletter: Tech Weekly',
     'This week in tech: AI breakthroughs, new JavaScript frameworks, and the latest in cloud computing.',
     'INBOX');

-- Sample Messages — WORK
INSERT INTO MESSAGE (title, body, category_code) VALUES
    ('Q2 Planning Meeting',
     'The Q2 planning meeting is scheduled for next Monday at 10:00 AM. Please review the attached agenda and come prepared with your team updates.',
     'WORK'),
    ('Project Alpha Update',
     'The development team has completed the first sprint. All user stories have been delivered. The demo is scheduled for Friday afternoon.',
     'WORK');

-- Sample Messages — PERSONAL
INSERT INTO MESSAGE (title, body, category_code) VALUES
    ('Weekend Plans',
     'Hey! Are you free this weekend? We are thinking of organizing a hiking trip to the mountains. Let me know if you can join.',
     'PERSONAL'),
    ('Happy Birthday!',
     'Wishing you a wonderful birthday! Hope you have an amazing day surrounded by family and friends.',
     'PERSONAL');
