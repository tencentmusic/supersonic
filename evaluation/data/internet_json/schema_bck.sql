create table Activity (
  actid INTEGER PRIMARY KEY,
  activity_name varchar(25)
);

create table Participates_in (
  stuid INTEGER,
  actid INTEGER,
  FOREIGN KEY(stuid) REFERENCES Student(StuID),
  FOREIGN KEY(actid) REFERENCES Activity(actid)
);

create table Faculty_Participates_in (
  FacID INTEGER,
  actid INTEGER,
  FOREIGN KEY(FacID) REFERENCES Faculty(FacID),
  FOREIGN KEY(actid) REFERENCES Activity(actid)
);

create table Student (
        StuID        INTEGER PRIMARY KEY,
        LName        VARCHAR(12),
        Fname        VARCHAR(12),
        Age      INTEGER,
        Sex      VARCHAR(1),
        Major        INTEGER,
        Advisor      INTEGER,
        city_code    VARCHAR(3)
 );

create table Faculty (
       FacID 	       INTEGER PRIMARY KEY,
       Lname		VARCHAR(15),
       Fname		VARCHAR(15),
       Rank		VARCHAR(15),
       Sex		VARCHAR(1),
       Phone		INTEGER,
       Room		VARCHAR(5),
       Building		VARCHAR(13)
);


insert into Faculty  values ( 1082, 'Giuliano', 'Mark', 'Instructor', 'M', 2424, '224', 'NEB');
insert into Faculty  values ( 1121, 'Goodrich', 'Michael', 'Professor', 'M', 3593, '219', 'NEB');
insert into Faculty  values ( 1148, 'Masson', 'Gerald', 'Professor', 'M', 3402, '224B', 'NEB');
insert into Faculty  values ( 1193, 'Jones', 'Stacey', 'Instructor', 'F', 3550, '224', 'NEB');
insert into Faculty  values ( 2192, 'Yarowsky', 'David', 'AsstProf', 'M', 6587, '324', 'NEB');
insert into Faculty  values ( 3457, 'Smith', 'Scott', 'AssocProf', 'M', 1035, '318', 'NEB');
insert into Faculty  values ( 4230, 'Houlahan', 'Joanne', 'Instructor', 'F', 1260, '328', 'NEB');
insert into Faculty  values ( 6112, 'Beach', 'Louis', 'Instructor', 'M', 1838, '207', 'NEB');
insert into Faculty  values ( 7712, 'Awerbuch', 'Baruch', 'Professor', 'M', 2105, '220', 'NEB');
insert into Faculty  values ( 7792, 'Brill', 'Eric', 'AsstProf', 'M', 2303, '324B', 'NEB');
insert into Faculty  values ( 7723, 'Taylor', 'Russell', 'Professor', 'M', 2435, '317', 'NEB');
insert into Faculty  values ( 8114, 'Angelopoulou', 'Ellie', 'Instructor', 'F', 2152, '316', 'NEB');
insert into Faculty  values ( 8423, 'Kumar', 'Subodh', 'AsstProf', 'M', 2522, '218', 'NEB');
insert into Faculty  values ( 8721, 'Wolff', 'Lawrence', 'AssocProf', 'M', 2342, '316', 'NEB');
insert into Faculty  values ( 8741, 'Salzberg', 'Steven', 'AssocProf', 'M', 2641,   '324A', 'NEB');
insert into Faculty  values ( 8918, 'Amir', 'Yair', 'AsstProf', 'M', 2672, '308', 'NEB');
insert into Faculty  values ( 9172, 'Kosaraju', 'Rao', 'Professor', 'M', 2757, '319', 'NEB');
insert into Faculty  values ( 9826, 'Delcher', 'Arthur', 'Instructor', 'M', 2956, '329', 'NEB');
insert into Faculty  values ( 1172, 'Runolfsson', 'Thordur', 'AssocProf', 'M', 3121, '119', 'Barton');
insert into Faculty  values ( 1177, 'Naiman', 'Daniel', 'Professor', 'M', 3571, '288', 'Krieger');
insert into Faculty  values ( 1823, 'Davidson', 'Frederic', 'Professor', 'M', 5629, '119', 'Barton');
insert into Faculty  values ( 2028, 'Brody', 'William', 'Professor', 'M', 6073, '119', 'Barton');
insert into Faculty  values ( 2119, 'Meyer', 'Gerard', 'Professor', 'M', 6350, '119', 'Barton');
insert into Faculty  values ( 2291, 'Scheinerman', 'Edward', 'Professor', 'M', 6654, '288', 'Krieger');
insert into Faculty  values ( 2311, 'Priebe', 'Carey', 'AsstProf', 'M', 6953, '288', 'Krieger');
insert into Faculty  values ( 2738, 'Fill', 'James', 'Professor', 'M', 8209, '288', 'Krieger');
insert into Faculty  values ( 2881, 'Goldman', 'Alan', 'Professor', 'M', 8335, '288', 'Krieger');
insert into Faculty  values ( 4432, 'Burzio', 'Luigi', 'Professor', 'M', 1813, '288', 'Krieger');
insert into Faculty  values ( 5718, 'Frank', 'Robert', 'AsstProf', 'M', 1751, '288', 'Krieger');
insert into Faculty  values ( 6182, 'Cheng', 'Cheng', 'AsstProf', 'M', 1856, '288', 'Krieger');
insert into Faculty  values ( 6191, 'Kaplan', 'Alexander', 'Professor', 'M', 1825, '119', 'Barton');
insert into Faculty  values ( 6330, 'Byrne', 'William', 'Instructor', 'M', 1691, '119', 'Barton');
insert into Faculty  values ( 6541, 'Han', 'Shih-Ping', 'Professor', 'M', 1914, '288', 'Krieger');
insert into Faculty  values ( 6910, 'Smolensky', 'Paul', 'Professor', 'M', 2072, '288', 'Krieger');
insert into Faculty  values ( 6925, 'Iglesias', 'Pablo', 'AsstProf', 'M', 2021, '119', 'Barton');
insert into Faculty  values ( 7134, 'Goutsias', 'John', 'Professor', 'M', 2184, '119', 'Barton');
insert into Faculty  values ( 7231, 'Rugh', 'Wilson', 'Professor', 'M', 2191, '119', 'Barton');
insert into Faculty  values ( 7271, 'Jelinek', 'Frederick', 'Professor', 'M', 2890, '119', 'Barton');
insert into Faculty  values ( 7506, 'Westgate', 'Charles', 'Professor', 'M', 2932, '119', 'Barton');
insert into Faculty  values ( 8102, 'James', 'Lancelot', 'AsstProf', 'M', 2792, '288', 'Krieger');
insert into Faculty  values ( 8118, 'Weinert', 'Howard', 'Professor', 'M', 3272, '119', 'Barton');
insert into Faculty  values ( 8122, 'Wierman', 'John', 'Professor', 'M', 3392,'288', 'Krieger');
insert into Faculty  values ( 8722, 'Cauwenberghs', 'Gert', 'AsstProf', 'M', 1372, '119', 'Barton');
insert into Faculty  values ( 8723, 'Andreou', 'Andreas', 'Professor', 'M', 1402, '119', 'Barton');
insert into Faculty  values ( 8772, 'Cowen', 'Lenore', 'AsstProf', 'F', 2870, '288', 'Krieger');
insert into Faculty  values ( 8791, 'McCloskey', 'Michael', 'Professor', 'M', 3440, '288', 'Krieger');
insert into Faculty  values ( 8989, 'Brent', 'Michael', 'AsstProf', 'M', 9373, '288', 'Krieger');
insert into Faculty  values ( 9011, 'Rapp', 'Brenda', 'AsstProf', 'F', 2032, '288', 'Krieger');
insert into Faculty  values ( 9191, 'Collins', 'Oliver', 'AssocProf', 'M', 5427, '119', 'Barton');
insert into Faculty  values ( 9199, 'Hughes', 'Brian', 'AssocProf', 'M', 5666, '119', 'Barton');
insert into Faculty  values ( 9210, 'Joseph', 'Richard', 'Professor', 'M', 5996, '119', 'Barton');
insert into Faculty  values ( 9514, 'Prince', 'Jerry', 'AssocProf', 'M', 5106, '119', 'Barton');
insert into Faculty  values ( 9823, 'Pang', 'Jong-Shi', 'Professor', 'M', 4366, '288', 'Krieger');
insert into Faculty  values ( 9824, 'Glaser', 'Robert', 'Instructor', 'M', 4396, '119', 'Barton');
insert into Faculty  values ( 9811, 'Wu', 'Colin', 'AsstProf', 'M', 2906, '288', 'Krieger');
insert into Faculty  values ( 9643, 'Legendre', 'Geraldine', 'AssocProf', 'F', 8972, '288', 'Krieger');
insert into Faculty  values ( 9379, 'Khurgin', 'Jacob', 'Professor', 'M', 1060, '119', 'Barton');
insert into Faculty  values ( 9922, 'Hall', 'Leslie', 'AsstProf', 'F', 7332, '288', 'Krieger');

insert into Student values ( 1001, 'Smith', 'Linda', 18, 'F', 600, 1121,'BAL');
 insert into Student values ( 1002, 'Kim', 'Tracy', 19, 'F', 600, 7712,'HKG');
 insert into Student values ( 1003, 'Jones', 'Shiela', 21, 'F', 600, 7792,'WAS');
 insert into Student values ( 1004, 'Kumar', 'Dinesh', 20, 'M', 600, 8423,'CHI');
 insert into Student values ( 1005, 'Gompers', 'Paul', 26, 'M', 600, 1121,'YYZ');
 insert into Student values ( 1006, 'Schultz', 'Andy', 18, 'M', 600, 1148,'BAL');
 insert into Student values ( 1007, 'Apap', 'Lisa', 18, 'F', 600, 8918,'PIT');
 insert into Student values ( 1008, 'Nelson', 'Jandy', 20, 'F', 600, 9172,'BAL');
 insert into Student values ( 1009, 'Tai', 'Eric', 19, 'M', 600, 2192,'YYZ');
 insert into Student values ( 1010, 'Lee', 'Derek', 17, 'M', 600, 2192,'HOU');
 insert into Student values ( 1011, 'Adams', 'David', 22, 'M', 600, 1148,'PHL');
 insert into Student values ( 1012, 'Davis', 'Steven', 20, 'M', 600, 7723,'PIT');
 insert into Student values ( 1014, 'Norris', 'Charles', 18, 'M', 600, 8741, 'DAL');
 insert into Student values ( 1015, 'Lee', 'Susan', 16, 'F', 600, 8721,'HKG');
 insert into Student values ( 1016, 'Schwartz', 'Mark', 17, 'M', 600, 2192,'DET');
 insert into Student values ( 1017, 'Wilson', 'Bruce', 27, 'M', 600, 1148,'LON');
 insert into Student values ( 1018, 'Leighton', 'Michael', 20, 'M', 600, 1121, 'PIT');
 insert into Student values ( 1019, 'Pang', 'Arthur', 18, 'M', 600, 2192,'WAS');
 insert into Student values ( 1020, 'Thornton', 'Ian', 22, 'M', 520, 7271,'NYC');
 insert into Student values ( 1021, 'Andreou', 'George', 19, 'M', 520, 8722, 'NYC');
 insert into Student values ( 1022, 'Woods', 'Michael', 17, 'M', 540, 8722,'PHL');
 insert into Student values ( 1023, 'Shieber', 'David', 20, 'M', 520, 8722,'NYC');
 insert into Student values ( 1024, 'Prater', 'Stacy', 18, 'F', 540, 7271,'BAL');
 insert into Student values ( 1025, 'Goldman', 'Mark', 18, 'M', 520, 7134,'PIT');
 insert into Student values ( 1026, 'Pang', 'Eric', 19, 'M', 520, 7134,'HKG');
 insert into Student values ( 1027, 'Brody', 'Paul', 18, 'M', 520, 8723,'LOS');
 insert into Student values ( 1028, 'Rugh', 'Eric', 20, 'M', 550, 2311,'ROC');
 insert into Student values ( 1029, 'Han', 'Jun', 17, 'M', 100, 2311,'PEK');
 insert into Student values ( 1030, 'Cheng', 'Lisa', 21, 'F', 550, 2311,'SFO');
 insert into Student values ( 1031, 'Smith', 'Sarah', 20, 'F', 550, 8772,'PHL');
 insert into Student values ( 1032, 'Brown', 'Eric', 20, 'M', 550, 8772,'ATL');
 insert into Student values ( 1033, 'Simms', 'William', 18, 'M', 550, 8772,'NAR');
 insert into Student values ( 1034, 'Epp', 'Eric', 18, 'M', 050, 5718,'BOS');
 insert into Student values ( 1035, 'Schmidt', 'Sarah', 26, 'F', 050, 5718,'WAS');

insert into Activity values ( 770  ,   'Mountain Climbing'  ) ;
insert into Activity values ( 771  ,   'Canoeing'  ) ;
insert into Activity values ( 772  ,   'Kayaking'  ) ;
insert into Activity values ( 773  ,   'Spelunking'  ) ;
insert into Activity values ( 777  ,   'Soccer'  ) ;
insert into Activity values ( 778  ,   'Baseball'  ) ;
insert into Activity values ( 780  ,   'Football'  ) ;
insert into Activity values ( 782  ,   'Volleyball'  ) ;
insert into Activity values ( 799  ,   'Bungee Jumping' ) ;
insert into Activity values ( 779  ,   'Accordion Ensemble' ) ;
insert into Activity values ( 784  ,   'Canasta'  ) ;
insert into Activity values ( 785  ,   'Chess'  ) ;
insert into Activity values ( 776  ,   'Extreme Canasta' ) ;
insert into Activity values ( 790  ,   'Crossword Puzzles'  ) ;
insert into Activity values ( 791  ,   'Proselytizing'  ) ;
insert into Activity values ( 796  ,   'Square Dancing' ) ;




insert into Participates_in values (1001  ,  770) ;
insert into Participates_in values (1001  ,  771) ;
insert into Participates_in values (1001  ,  777) ;

insert into Participates_in values (1002  ,  772) ;
insert into Participates_in values (1002  ,  771) ;

insert into Participates_in values (1003  ,  778) ;


insert into Participates_in values (1004  ,  780) ;
insert into Participates_in values (1004  ,  782) ;
insert into Participates_in values (1004  ,  778) ;
insert into Participates_in values (1004  ,  777) ;

insert into Participates_in values (1005  ,  770) ;


insert into Participates_in values (1006  ,  773) ;


insert into Participates_in values (1007  ,  773) ;
insert into Participates_in values (1007  ,  784) ;

insert into Participates_in values (1008  ,  785) ;
insert into Participates_in values (1008  ,  773) ;
insert into Participates_in values (1008  ,  780) ;
insert into Participates_in values (1008  ,  790) ;

insert into Participates_in values (1009  ,  778) ;
insert into Participates_in values (1009  ,  777) ;
insert into Participates_in values (1009  ,  782) ;

insert into Participates_in values (1010  ,  780) ;


insert into Participates_in values (1011  ,  780) ;


insert into Participates_in values (1012  ,  780) ;


insert into Participates_in values (1014  ,  780) ;
insert into Participates_in values (1014  ,  777) ;
insert into Participates_in values (1014  ,  778) ;
insert into Participates_in values (1014  ,  782) ;
insert into Participates_in values (1014  ,  770) ;
insert into Participates_in values (1014  ,  772) ;

insert into Participates_in values (1015  ,  785) ;


insert into Participates_in values (1016  ,  791) ;
insert into Participates_in values (1016  ,  772) ;

insert into Participates_in values (1017  ,  791) ;
insert into Participates_in values (1017  ,  771) ;
insert into Participates_in values (1017  ,  770) ;

insert into Participates_in values (1018  ,  790) ;
insert into Participates_in values (1018  ,  785) ;
insert into Participates_in values (1018  ,  784) ;
insert into Participates_in values (1018  ,  777) ;
insert into Participates_in values (1018  ,  772) ;
insert into Participates_in values (1018  ,  770) ;

insert into Participates_in values (1019  ,  785) ;
insert into Participates_in values (1019  ,  790) ;

insert into Participates_in values (1020  ,  780) ;


insert into Participates_in values (1021  ,  780) ;
insert into Participates_in values (1021  ,  776) ;

insert into Participates_in values (1022  ,  782) ;
insert into Participates_in values (1022  ,  790) ;

insert into Participates_in values (1023  ,  790) ;
insert into Participates_in values (1023  ,  776) ;

insert into Participates_in values (1024  ,  778) ;
insert into Participates_in values (1024  ,  777) ;
insert into Participates_in values (1024  ,  780) ;

insert into Participates_in values (1025  ,  780) ;
insert into Participates_in values (1025  ,  777) ;
insert into Participates_in values (1025  ,  770) ;

insert into Participates_in values (1028  ,  785) ;


insert into Participates_in values (1029  ,  785) ;
insert into Participates_in values (1029  ,  790) ;

insert into Participates_in values (1030  ,  780) ;
insert into Participates_in values (1030  ,  790) ;

insert into Participates_in values (1033  ,  780) ;

insert into Participates_in values (1034  ,  780) ;
insert into Participates_in values (1034  ,  777) ;
insert into Participates_in values (1034  ,  772) ;
insert into Participates_in values (1034  ,  771) ;

insert into Participates_in values (1035  ,  777) ;
insert into Participates_in values (1035  ,  780) ;
insert into Participates_in values (1035  ,  784) ;






insert into Faculty_Participates_in values ( 1082, 784) ;
insert into Faculty_Participates_in values ( 1082, 785) ;
insert into Faculty_Participates_in values ( 1082, 790) ;

insert into Faculty_Participates_in values ( 1121, 771) ;
insert into Faculty_Participates_in values ( 1121, 777) ;
insert into Faculty_Participates_in values ( 1121, 770) ;

insert into Faculty_Participates_in values ( 1193, 790) ;
insert into Faculty_Participates_in values ( 1193, 796) ;
insert into Faculty_Participates_in values ( 1193, 773) ;

insert into Faculty_Participates_in values ( 2192, 773) ;
insert into Faculty_Participates_in values ( 2192, 790) ;
insert into Faculty_Participates_in values ( 2192, 778) ;

insert into Faculty_Participates_in values ( 3457, 782) ;
insert into Faculty_Participates_in values ( 3457, 771) ;
insert into Faculty_Participates_in values ( 3457, 784) ;

insert into Faculty_Participates_in values ( 4230, 790) ;
insert into Faculty_Participates_in values ( 4230, 785) ;

insert into Faculty_Participates_in values ( 6112, 785) ;
insert into Faculty_Participates_in values ( 6112, 772) ;

insert into Faculty_Participates_in values ( 7723, 785) ;
insert into Faculty_Participates_in values ( 7723, 770) ;

insert into Faculty_Participates_in values ( 8114, 776) ;

insert into Faculty_Participates_in values ( 8721, 770) ;
insert into Faculty_Participates_in values ( 8721, 780) ;

insert into Faculty_Participates_in values ( 8741, 780) ;
insert into Faculty_Participates_in values ( 8741, 790) ;

insert into Faculty_Participates_in values ( 8918, 780) ;
insert into Faculty_Participates_in values ( 8918, 782) ;
insert into Faculty_Participates_in values ( 8918, 771) ;

insert into Faculty_Participates_in values ( 2881, 790) ;
insert into Faculty_Participates_in values ( 2881, 784) ;

insert into Faculty_Participates_in values ( 4432, 770) ;
insert into Faculty_Participates_in values ( 4432, 771) ;

insert into Faculty_Participates_in values ( 5718, 776) ;


insert into Faculty_Participates_in values ( 6182, 776) ;
insert into Faculty_Participates_in values ( 6182, 785) ;

insert into Faculty_Participates_in values ( 1177, 790) ;
insert into Faculty_Participates_in values ( 1177, 770) ;
insert into Faculty_Participates_in values ( 1177, 770) ;

insert into Faculty_Participates_in values ( 9922, 796) ;
