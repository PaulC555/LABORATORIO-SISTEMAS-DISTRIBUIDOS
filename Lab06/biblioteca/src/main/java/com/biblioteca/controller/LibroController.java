package com.biblioteca.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.biblioteca.model.Libro;

@RestController
@RequestMapping("/libros")
public class LibroController {

    List<Libro> libros = new ArrayList<>();

    // LISTAR
    @GetMapping
    public List<Libro> listar() {

        return libros;
    }

    // AGREGAR
    @PostMapping
    public String agregar(@RequestBody Libro libro) {

        libros.add(libro);

        return "Libro agregado";
    }

    // BUSCAR POR ID
    @GetMapping("/{id}")
    public Libro buscar(@PathVariable int id) {

        for (Libro libro : libros) {

            if (libro.getId() == id) {

                return libro;
            }
        }

        return null;
    }

    // ELIMINAR
    @DeleteMapping("/{id}")
    public String eliminar(@PathVariable int id) {

        for (Libro libro : libros) {

            if (libro.getId() == id) {

                libros.remove(libro);

                return "Libro eliminado";
            }
        }

        return "Libro no encontrado";
    }

}