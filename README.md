# zen-demo-java

## Setup : 
Create postgres table : 
```sql
CREATE TABLE IF NOT EXISTS pets (
    pet_id SERIAL PRIMARY KEY,
    pet_name VARCHAR(250) NOT NULL,
    owner VARCHAR(250) NOT NULL
);
```

Start server on port 8080 :
```shell
make download
AIKIDO_TOKEN="token" make run
```