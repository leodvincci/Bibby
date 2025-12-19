
-- Bibby seed data (DEV)
-- Contains: bookcases, shelves, books, authors, book_authors
-- IDs offset by +100 to avoid collisions with existing test data.


INSERT INTO bookcases (
    bookcase_id,
    book_capacity,
    shelf_capacity,
    bookcase_description,
    bookcase_label,
    bookcase_location
) VALUES
      (101, 30, 5, 'Basement west wall main shelf for tech + reference.', 'WestWall-A', 'Basement'),
      (102, 25, 6, 'Basement overflow: paperbacks and misc reads.', 'WestWall-B', 'Basement'),
      (103, 28, 4, 'Near desk area: currently reading + notes.', 'Desk-1', 'Basement'),
      (104, 22, 5, 'Office window wall: work + MBA books.', 'WindowWall-1', 'Office'),
      (105, 24, 5, 'Office north wall: software + architecture.', 'NorthWall-1', 'Office'),
      (106, 18, 4, 'Office small shelf: quick references.', 'NorthWall-2', 'Office'),
      (107, 20, 5, 'Living room display: favorites + conversation starters.', 'CouchSide-1', 'LivingRoom'),
      (108, 16, 3, 'Living room corner shelf: art + design books.', 'Corner-A', 'LivingRoom'),
      (109, 26, 6, 'Bedroom main shelf: personal development + journals.', 'Main-1', 'Bedroom'),
      (110, 20, 4, 'Bedroom secondary: fiction + leisure.', 'Main-2', 'Bedroom'),
      (111, 32, 6, 'Garage storage rack: boxes and long-term storage.', 'Rack-01', 'Garage'),
      (112, 28, 7, 'Storage room bulk shelf: archived books.', 'Bulk-01', 'Storage');



INSERT INTO shelves (
    shelf_id,
    bookcase_id,
    shelf_position,
    book_capacity,
    shelf_label,
    shelf_description
) VALUES
-- Bookcase 101 (5 shelves, 30 books/shelf)
(101, 101, 1, 30, 'S01', 'Top shelf'),
(102, 101, 2, 30, 'S02', 'Upper shelf'),
(103, 101, 3, 30, 'S03', 'Middle shelf'),
(104, 101, 4, 30, 'S04', 'Lower shelf'),
(105, 101, 5, 30, 'S05', 'Bottom shelf'),

-- Bookcase 102 (6 shelves, 25 books/shelf)
(106, 102, 1, 25, 'S01', 'Top shelf'),
(107, 102, 2, 25, 'S02', 'Upper shelf'),
(108, 102, 3, 25, 'S03', 'Upper-middle shelf'),
(109, 102, 4, 25, 'S04', 'Lower-middle shelf'),
(110, 102, 5, 25, 'S05', 'Lower shelf'),
(111, 102, 6, 25, 'S06', 'Bottom shelf'),

-- Bookcase 103 (4 shelves, 28 books/shelf)
(112, 103, 1, 28, 'S01', 'Top shelf'),
(113, 103, 2, 28, 'S02', 'Upper shelf'),
(114, 103, 3, 28, 'S03', 'Lower shelf'),
(115, 103, 4, 28, 'S04', 'Bottom shelf'),

-- Bookcase 104 (5 shelves, 22 books/shelf)
(116, 104, 1, 22, 'S01', 'Top shelf'),
(117, 104, 2, 22, 'S02', 'Upper shelf'),
(118, 104, 3, 22, 'S03', 'Middle shelf'),
(119, 104, 4, 22, 'S04', 'Lower shelf'),
(120, 104, 5, 22, 'S05', 'Bottom shelf'),

-- Bookcase 105 (5 shelves, 24 books/shelf)
(121, 105, 1, 24, 'S01', 'Top shelf'),
(122, 105, 2, 24, 'S02', 'Upper shelf'),
(123, 105, 3, 24, 'S03', 'Middle shelf'),
(124, 105, 4, 24, 'S04', 'Lower shelf'),
(125, 105, 5, 24, 'S05', 'Bottom shelf'),

-- Bookcase 106 (4 shelves, 18 books/shelf)
(126, 106, 1, 18, 'S01', 'Top shelf'),
(127, 106, 2, 18, 'S02', 'Upper shelf'),
(128, 106, 3, 18, 'S03', 'Lower shelf'),
(129, 106, 4, 18, 'S04', 'Bottom shelf'),

-- Bookcase 107 (5 shelves, 20 books/shelf)
(130, 107, 1, 20, 'S01', 'Top shelf'),
(131, 107, 2, 20, 'S02', 'Upper shelf'),
(132, 107, 3, 20, 'S03', 'Middle shelf'),
(133, 107, 4, 20, 'S04', 'Lower shelf'),
(134, 107, 5, 20, 'S05', 'Bottom shelf'),

-- Bookcase 108 (3 shelves, 16 books/shelf)
(135, 108, 1, 16, 'S01', 'Top shelf'),
(136, 108, 2, 16, 'S02', 'Middle shelf'),
(137, 108, 3, 16, 'S03', 'Bottom shelf'),

-- Bookcase 109 (6 shelves, 26 books/shelf)
(138, 109, 1, 26, 'S01', 'Top shelf'),
(139, 109, 2, 26, 'S02', 'Upper shelf'),
(140, 109, 3, 26, 'S03', 'Upper-middle shelf'),
(141, 109, 4, 26, 'S04', 'Lower-middle shelf'),
(142, 109, 5, 26, 'S05', 'Lower shelf'),
(143, 109, 6, 26, 'S06', 'Bottom shelf'),

-- Bookcase 110 (4 shelves, 20 books/shelf)
(144, 110, 1, 20, 'S01', 'Top shelf'),
(145, 110, 2, 20, 'S02', 'Upper shelf'),
(146, 110, 3, 20, 'S03', 'Lower shelf'),
(147, 110, 4, 20, 'S04', 'Bottom shelf'),

-- Bookcase 111 (6 shelves, 32 books/shelf)
(148, 111, 1, 32, 'S01', 'Top shelf'),
(149, 111, 2, 32, 'S02', 'Upper shelf'),
(150, 111, 3, 32, 'S03', 'Upper-middle shelf'),
(151, 111, 4, 32, 'S04', 'Lower-middle shelf'),
(152, 111, 5, 32, 'S05', 'Lower shelf'),
(153, 111, 6, 32, 'S06', 'Bottom shelf'),

-- Bookcase 112 (7 shelves, 28 books/shelf)
(154, 112, 1, 28, 'S01', 'Top shelf'),
(155, 112, 2, 28, 'S02', 'Upper shelf'),
(156, 112, 3, 28, 'S03', 'Upper-middle shelf'),
(157, 112, 4, 28, 'S04', 'Middle shelf'),
(158, 112, 5, 28, 'S05', 'Lower-middle shelf'),
(159, 112, 6, 28, 'S06', 'Lower shelf'),
(160, 112, 7, 28, 'S07', 'Bottom shelf');



INSERT INTO books (
    created_at,
    edition,
    publication_year,
    updated_at,
    book_id,
    shelf_id,
    availability_status,
    description,
    genre,
    isbn,
    publisher,
    title
) VALUES
      (CURRENT_DATE, 1, 2008, CURRENT_DATE, 101, 101, 'AVAILABLE',   'Seed tech book data.', 'Software Engineering', '9780000010018', 'Prentice Hall',         'Clean Code'),
      (CURRENT_DATE, 1, 2017, CURRENT_DATE, 102, 102, 'AVAILABLE',   'Seed tech book data.', 'Software Engineering', '9780000010025', 'Pearson',               'Clean Architecture'),
      (CURRENT_DATE, 1, 2003, CURRENT_DATE, 103, 103, 'AVAILABLE',   'Seed tech book data.', 'Software Engineering', '9780000010032', 'Addison-Wesley',        'Domain-Driven Design'),
      (CURRENT_DATE, 2, 2018, CURRENT_DATE, 104, 104, 'AVAILABLE',   'Seed tech book data.', 'Software Engineering', '9780000010049', 'Addison-Wesley',        'Refactoring'),
      (CURRENT_DATE, 1, 1994, CURRENT_DATE, 105, 105, 'AVAILABLE',   'Seed tech book data.', 'Software Engineering', '9780000010056', 'Addison-Wesley',        'Design Patterns'),
      (CURRENT_DATE, 3, 2018, CURRENT_DATE, 106, 106, 'AVAILABLE',   'Seed tech book data.', 'Java',                 '9780000010063', 'Addison-Wesley',        'Effective Java'),
      (CURRENT_DATE, 1, 2006, CURRENT_DATE, 107, 107, 'AVAILABLE',   'Seed tech book data.', 'Java',                 '9780000010070', 'Addison-Wesley',        'Java Concurrency in Practice'),
      (CURRENT_DATE, 2, 2019, CURRENT_DATE, 108, 108, 'AVAILABLE',   'Seed tech book data.', 'Software Engineering', '9780000010087', 'Addison-Wesley',        'The Pragmatic Programmer'),
      (CURRENT_DATE, 2, 2020, CURRENT_DATE, 109, 109, 'AVAILABLE',   'Seed tech book data.', 'Software Engineering', '9780000010094', 'O''Reilly Media',       'Head First Design Patterns'),
      (CURRENT_DATE, 6, 2022, CURRENT_DATE, 110, 110, 'AVAILABLE',   'Seed tech book data.', 'Java',                 '9780000010100', 'Manning',               'Spring in Action'),

      (CURRENT_DATE, 1, 2017, CURRENT_DATE, 111, 111, 'AVAILABLE',   'Seed tech book data.', 'Data Engineering',      '9780000010117', 'O''Reilly Media',       'Designing Data-Intensive Applications'),
      (CURRENT_DATE, 1, 2013, CURRENT_DATE, 112, 112, 'AVAILABLE',   'Seed tech book data.', 'DevOps',               '9780000010124', 'IT Revolution',         'The Phoenix Project'),
      (CURRENT_DATE, 1, 2018, CURRENT_DATE, 113, 113, 'AVAILABLE',   'Seed tech book data.', 'DevOps',               '9780000010131', 'IT Revolution',         'Accelerate'),
      (CURRENT_DATE, 2, 2021, CURRENT_DATE, 114, 114, 'AVAILABLE',   'Seed tech book data.', 'DevOps',               '9780000010148', 'IT Revolution',         'The DevOps Handbook'),
      (CURRENT_DATE, 1, 2016, CURRENT_DATE, 115, 115, 'AVAILABLE',   'Seed tech book data.', 'SRE',                  '9780000010155', 'O''Reilly Media',       'Site Reliability Engineering'),
      (CURRENT_DATE, 4, 2009, CURRENT_DATE, 116, 116, 'AVAILABLE',   'Seed tech book data.', 'Computer Science',      '9780000010162', 'MIT Press',             'Introduction to Algorithms'),
      (CURRENT_DATE, 1, 2016, CURRENT_DATE, 117, 117, 'AVAILABLE',   'Seed tech book data.', 'Computer Science',      '9780000010179', 'Manning',               'Grokking Algorithms'),
      (CURRENT_DATE, 3, 2018, CURRENT_DATE, 118, 118, 'AVAILABLE',   'Seed tech book data.', 'JavaScript',            '9780000010186', 'No Starch Press',       'Eloquent JavaScript'),
      (CURRENT_DATE, 1, 2020, CURRENT_DATE, 119, 119, 'CHECKED_OUT', 'Seed tech book data.', 'JavaScript',            '9780000010193', 'Independently Published','You Don''t Know JS Yet'),
      (CURRENT_DATE, 2, 1996, CURRENT_DATE, 120, 120, 'AVAILABLE',   'Seed tech book data.', 'Computer Science',      '9780000010209', 'MIT Press',             'Structure and Interpretation of Computer Programs'),

      (CURRENT_DATE, 1, 2018, CURRENT_DATE, 121, 121, 'AVAILABLE',   'Seed tech book data.', 'Operating Systems',     '9780000010216', 'Arpaci-Dusseau',        'Operating Systems: Three Easy Pieces'),
      (CURRENT_DATE, 8, 2020, CURRENT_DATE, 122, 122, 'AVAILABLE',   'Seed tech book data.', 'Networking',            '9780000010223', 'Pearson',               'Computer Networking: A Top-Down Approach'),
      (CURRENT_DATE, 1, 2002, CURRENT_DATE, 123, 123, 'AVAILABLE',   'Seed tech book data.', 'Software Engineering',  '9780000010230', 'Addison-Wesley',        'Patterns of Enterprise Application Architecture'),
      (CURRENT_DATE, 2, 2017, CURRENT_DATE, 124, 124, 'AVAILABLE',   'Seed tech book data.', 'Software Engineering',  '9780000010247', 'Pragmatic Bookshelf',   'Release It!'),
      (CURRENT_DATE, 2, 2021, CURRENT_DATE, 125, 125, 'AVAILABLE',   'Seed tech book data.', 'Software Engineering',  '9780000010254', 'O''Reilly Media',       'Building Microservices'),
      (CURRENT_DATE, 3, 2022, CURRENT_DATE, 126, 126, 'AVAILABLE',   'Seed tech book data.', 'Cloud',                '9780000010261', 'O''Reilly Media',       'Kubernetes: Up & Running'),
      (CURRENT_DATE, 1, 2020, CURRENT_DATE, 127, 127, 'AVAILABLE',   'Seed tech book data.', 'Cloud',                '9780000010278', 'Independently Published','Docker Deep Dive'),
      (CURRENT_DATE, 1, 2019, CURRENT_DATE, 128, 128, 'AVAILABLE',   'Seed tech book data.', 'Data Science',          '9780000010285', 'O''Reilly Media',       'Data Science from Scratch'),
      (CURRENT_DATE, 2, 2019, CURRENT_DATE, 129, 129, 'AVAILABLE',   'Seed tech book data.', 'Python',               '9780000010292', 'No Starch Press',       'Python Crash Course'),
      (CURRENT_DATE, 2, 2022, CURRENT_DATE, 130, 130, 'AVAILABLE',   'Seed tech book data.', 'Python',               '9780000010308', 'O''Reilly Media',       'Fluent Python'),

      (CURRENT_DATE, 1, 2019, CURRENT_DATE, 131, 131, 'AVAILABLE',   'Seed tech book data.', 'TypeScript',           '9780000010315', 'O''Reilly Media',       'Effective TypeScript'),
      (CURRENT_DATE, 1, 2019, CURRENT_DATE, 132, 132, 'AVAILABLE',   'Seed tech book data.', 'Rust',                 '9780000010322', 'No Starch Press',       'The Rust Programming Language'),
      (CURRENT_DATE, 1, 2016, CURRENT_DATE, 133, 133, 'AVAILABLE',   'Seed tech book data.', 'Go',                   '9780000010339', 'Manning',               'Go in Practice'),
      (CURRENT_DATE, 6, 2020, CURRENT_DATE, 134, 134, 'AVAILABLE',   'Seed tech book data.', 'SQL',                  '9780000010346', 'O''Reilly Media',       'Learning SQL'),
      (CURRENT_DATE, 1, 2015, CURRENT_DATE, 135, 135, 'AVAILABLE',   'Seed tech book data.', 'SQL',                  '9780000010353', 'Markus Winand',         'SQL Performance Explained'),
      (CURRENT_DATE, 1, 1995, CURRENT_DATE, 136, 136, 'AVAILABLE',   'Seed tech book data.', 'Software Engineering', '9780000010360', 'Addison-Wesley',        'The Mythical Man-Month'),
      (CURRENT_DATE, 1, 2011, CURRENT_DATE, 137, 137, 'AVAILABLE',   'Seed tech book data.', 'Software Engineering', '9780000010377', 'Prentice Hall',         'The Clean Coder'),
      (CURRENT_DATE, 1, 2004, CURRENT_DATE, 138, 138, 'AVAILABLE',   'Seed tech book data.', 'Software Engineering', '9780000010384', 'Prentice Hall',         'Working Effectively with Legacy Code'),
      (CURRENT_DATE, 1, 2010, CURRENT_DATE, 139, 139, 'AVAILABLE',   'Seed tech book data.', 'DevOps',               '9780000010391', 'Addison-Wesley',        'Continuous Delivery'),
      (CURRENT_DATE, 1, 2002, CURRENT_DATE, 140, 140, 'AVAILABLE',   'Seed tech book data.', 'Software Engineering', '9780000010407', 'Addison-Wesley',        'Test-Driven Development: By Example');



INSERT INTO authors (author_id, first_name, last_name) VALUES
                                                           (101, 'Robert',   'Martin'),
                                                           (102, 'Martin',   'Fowler'),
                                                           (103, 'Eric',     'Evans'),
                                                           (104, 'Erich',    'Gamma'),
                                                           (105, 'Richard',  'Helm'),
                                                           (106, 'Ralph',    'Johnson'),
                                                           (107, 'John',     'Vlissides'),
                                                           (108, 'Joshua',   'Bloch'),
                                                           (109, 'Brian',    'Goetz'),
                                                           (110, 'Andy',     'Hunt'),
                                                           (111, 'Dave',     'Thomas'),
                                                           (112, 'Kathy',    'Sierra'),
                                                           (113, 'Bert',     'Bates'),
                                                           (114, 'Craig',    'Walls'),
                                                           (115, 'Martin',   'Kleppmann'),
                                                           (116, 'Gene',     'Kim'),
                                                           (117, 'Kevin',    'Behr'),
                                                           (118, 'George',   'Spafford'),
                                                           (119, 'Nicole',   'Forsgren'),
                                                           (120, 'Jez',      'Humble'),
                                                           (121, 'Gene',     'Kim'),
                                                           (122, 'Patrick',  'Debois'),
                                                           (123, 'John',     'Willis'),
                                                           (124, 'John',     'Allspaw'),
                                                           (125, 'Niall',    'Murphy'),
                                                           (126, 'Thomas',   'Haver'),
                                                           (127, 'Len',      'Bass'),
                                                           (128, 'Peter',    'Seibel'),
                                                           (129, 'Thomas',   'Cormen'),
                                                           (130, 'Charles',  'Leiserson'),
                                                           (131, 'Ronald',   'Rivest'),
                                                           (132, 'Clifford', 'Stein'),
                                                           (133, 'Aditya',   'Bhargava'),
                                                           (134, 'Marijn',   'Haverbeke'),
                                                           (135, 'Kyle',     'Simpson'),
                                                           (136, 'Harold',   'Abelson'),
                                                           (137, 'Gerald',   'Sussman'),
                                                           (138, 'Barbara',  'Liskov'),
                                                           (139, 'John',     'Guttag'),
                                                           (140, 'Michael',  'Nygard');



INSERT INTO book_authors (author_id, book_id) VALUES
                                                  (101, 101),
                                                  (101, 102),
                                                  (103, 103),
                                                  (102, 104),

                                                  (104, 105),(105, 105),(106, 105),(107, 105),

                                                  (108, 106),
                                                  (109, 107),
                                                  (110, 108),(111, 108),
                                                  (112, 109),(113, 109),
                                                  (114, 110),
                                                  (115, 111),

                                                  (116, 112),(117, 112),(118, 112),
                                                  (119, 113),(120, 113),(116, 113),
                                                  (116, 114),(120, 114),(122, 114),(123, 114),

                                                  (124, 115),(125, 115),(126, 115),(127, 115),(128, 115),

                                                  (129, 116),(130, 116),(131, 116),(132, 116),

                                                  (133, 117),
                                                  (134, 118),
                                                  (135, 119),
                                                  (136, 120),(137, 120),

                                                  (138, 121),(139, 121),

                                                  (128, 122),
                                                  (102, 123),
                                                  (140, 124),
                                                  (128, 125),

                                                  (124, 126),(125, 126),(126, 126),(127, 126),(128, 126),(140, 126),

                                                  (128, 127),
                                                  (128, 128),
                                                  (128, 129),
                                                  (128, 130),
                                                  (128, 131),
                                                  (128, 132),
                                                  (128, 133),
                                                  (128, 134),
                                                  (128, 135),
                                                  (128, 136),

                                                  (101, 137),
                                                  (128, 138),
                                                  (120, 139),
                                                  (102, 140);
