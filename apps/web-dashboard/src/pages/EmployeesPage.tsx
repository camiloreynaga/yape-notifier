import { useEffect, useState } from 'react';
import { apiService } from '@/services/api';
import type { User } from '@/types';
import { format } from 'date-fns';
import { es } from 'date-fns/locale';
import { Plus, Edit, Trash2, Power, PowerOff, User as UserIcon, Key, Copy, Check, Eye, EyeOff, RefreshCw } from 'lucide-react';

export default function EmployeesPage() {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [editingUser, setEditingUser] = useState<User | null>(null);
  const [formData, setFormData] = useState({
    name: '',
    email: '',
    role: 'captador' as 'admin' | 'captador',
    is_active: true,
  });
  const [generatedPin, setGeneratedPin] = useState<string | null>(null);
  const [copiedPin, setCopiedPin] = useState(false);
  const [error, setError] = useState('');
  const [revealedPasswords, setRevealedPasswords] = useState<Record<number, boolean>>({});
  const [generatedPassword, setGeneratedPassword] = useState<string | null>(null);
  const [copiedPassword, setCopiedPassword] = useState(false);

  useEffect(() => {
    loadUsers();
  }, []);

  const loadUsers = async () => {
    setLoading(true);
    try {
      const userList = await apiService.getUsers();
      setUsers(userList);
    } catch (error) {
      console.error('Error loading users:', error);
      setError('Error al cargar empleados');
    } finally {
      setLoading(false);
    }
  };

  const handleCreate = () => {
    setEditingUser(null);
    setFormData({
      name: '',
      email: '',
      role: 'captador',
      is_active: true,
    });
    setGeneratedPin(null);
    setGeneratedPassword(null);
    setError('');
    setShowModal(true);
  };

  const handleEdit = (user: User) => {
    setEditingUser(user);
    setFormData({
      name: user.name,
      email: user.email,
      role: user.role === 'admin' || user.role === 'captador' ? user.role : 'captador',
      is_active: user.is_active ?? true,
    });
    setGeneratedPin(null);
    setGeneratedPassword(null);
    setError('');
    setShowModal(true);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    try {
      if (editingUser) {
        await apiService.updateUser(editingUser.id, formData);
      } else {
        const result = await apiService.createUser(formData);
        setGeneratedPin(result.pin);
        setGeneratedPassword(result.password);
      }
      // Keep the modal open if a credential was generated so the admin can copy it
      if (editingUser || (!generatedPin && !generatedPassword)) {
        setShowModal(false);
      }
      loadUsers();
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string; errors?: Record<string, string[]> } } };
      const errorMessage = err.response?.data?.message || 
        Object.values(err.response?.data?.errors || {}).flat().join(', ') ||
        'Error al guardar empleado';
      setError(errorMessage);
    }
  };

  const handleDelete = async (user: User) => {
    if (!confirm(`¿Estás seguro de eliminar a ${user.name}?`)) {
      return;
    }

    try {
      await apiService.deleteUser(user.id);
      loadUsers();
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string } } };
      alert(err.response?.data?.message || 'Error al eliminar empleado');
    }
  };

  const handleToggleStatus = async (user: User) => {
    try {
      await apiService.updateUser(user.id, {
        is_active: !user.is_active,
      });
      loadUsers();
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string } } };
      alert(err.response?.data?.message || 'Error al actualizar estado');
    }
  };

  const handleRegeneratePin = async (user: User) => {
    if (!confirm(`¿Regenerar PIN para ${user.name}? El PIN actual dejará de funcionar.`)) {
      return;
    }

    try {
      const result = await apiService.regenerateUserPin(user.id);
      setGeneratedPin(result.pin);
      setShowModal(true);
      setEditingUser(user);
      loadUsers();
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string } } };
      alert(err.response?.data?.message || 'Error al regenerar PIN');
    }
  };

  const copyPinToClipboard = () => {
    if (generatedPin) {
      navigator.clipboard.writeText(generatedPin);
      setCopiedPin(true);
      setTimeout(() => setCopiedPin(false), 2000);
    }
  };

  const handleRegeneratePassword = async (user: User) => {
    if (!confirm(`¿Regenerar contraseña para ${user.name}? La contraseña actual dejará de funcionar.`)) {
      return;
    }
    try {
      const result = await apiService.regenerateUserPassword(user.id);
      setGeneratedPassword(result.password);
      setGeneratedPin(null);
      setShowModal(true);
      setEditingUser(user);
      loadUsers();
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string } } };
      alert(err.response?.data?.message || 'Error al regenerar contraseña');
    }
  };

  const copyPasswordToClipboard = () => {
    if (generatedPassword) {
      navigator.clipboard.writeText(generatedPassword);
      setCopiedPassword(true);
      setTimeout(() => setCopiedPassword(false), 2000);
    }
  };

  const togglePasswordReveal = (userId: number) => {
    setRevealedPasswords((prev) => ({ ...prev, [userId]: !prev[userId] }));
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-3xl font-bold text-gray-900">Empleados</h1>
        <button
          onClick={handleCreate}
          className="btn btn-primary flex items-center gap-2"
        >
          <Plus className="h-5 w-5" />
          Agregar Empleado
        </button>
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
          {error}
        </div>
      )}

      <div className="bg-white rounded-lg shadow overflow-hidden">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Nombre
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Email
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Rol
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                PIN
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Contraseña
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Estado
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Creado
              </th>
              <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                Acciones
              </th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {users.length === 0 ? (
              <tr>
                <td colSpan={8} className="px-6 py-4 text-center text-gray-500">
                  No hay empleados registrados
                </td>
              </tr>
            ) : (
              users.map((user) => (
                <tr key={user.id} className="hover:bg-gray-50">
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="flex items-center">
                      <UserIcon className="h-5 w-5 text-gray-400 mr-2" />
                      <span className="text-sm font-medium text-gray-900">{user.name}</span>
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {user.email}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <span className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${
                      user.role === 'admin' 
                        ? 'bg-purple-100 text-purple-800' 
                        : 'bg-blue-100 text-blue-800'
                    }`}>
                      {user.role === 'admin' ? 'Administrador' : 'Captador'}
                    </span>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    {user.pin ? (
                      <div className="flex items-center gap-2">
                        <code className="text-sm font-mono bg-gray-100 px-2 py-1 rounded">
                          {user.pin}
                        </code>
                        <button
                          onClick={() => handleRegeneratePin(user)}
                          className="text-primary-600 hover:text-primary-800"
                          title="Regenerar PIN"
                        >
                          <Key className="h-4 w-4" />
                        </button>
                      </div>
                    ) : (
                      <span className="text-sm text-gray-400">Sin PIN</span>
                    )}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    {user.role === 'admin' ? (
                      user.password_visible ? (
                        <div className="flex items-center gap-2">
                          <code className="text-sm font-mono bg-gray-100 px-2 py-1 rounded">
                            {revealedPasswords[user.id] ? user.password_visible : '••••••••'}
                          </code>
                          <button
                            onClick={() => togglePasswordReveal(user.id)}
                            className="text-gray-500 hover:text-gray-800"
                            title={revealedPasswords[user.id] ? 'Ocultar' : 'Ver contraseña'}
                          >
                            {revealedPasswords[user.id] ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                          </button>
                          <button
                            onClick={() => {
                              navigator.clipboard.writeText(user.password_visible!);
                            }}
                            className="text-gray-500 hover:text-gray-800"
                            title="Copiar contraseña"
                          >
                            <Copy className="h-4 w-4" />
                          </button>
                          <button
                            onClick={() => handleRegeneratePassword(user)}
                            className="text-primary-600 hover:text-primary-800"
                            title="Regenerar contraseña"
                          >
                            <RefreshCw className="h-4 w-4" />
                          </button>
                        </div>
                      ) : (
                        <button
                          onClick={() => handleRegeneratePassword(user)}
                          className="text-sm text-primary-600 hover:text-primary-800 underline"
                        >
                          Generar contraseña
                        </button>
                      )
                    ) : (
                      <span className="text-sm text-gray-400">—</span>
                    )}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    {user.is_active ? (
                      <span className="px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-green-100 text-green-800">
                        Activo
                      </span>
                    ) : (
                      <span className="px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-red-100 text-red-800">
                        Inactivo
                      </span>
                    )}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {format(new Date(user.created_at), 'dd/MM/yyyy', { locale: es })}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                    <div className="flex justify-end gap-2">
                      <button
                        onClick={() => handleToggleStatus(user)}
                        className="text-gray-600 hover:text-gray-900"
                        title={user.is_active ? 'Desactivar' : 'Activar'}
                      >
                        {user.is_active ? (
                          <PowerOff className="h-5 w-5" />
                        ) : (
                          <Power className="h-5 w-5" />
                        )}
                      </button>
                      <button
                        onClick={() => handleEdit(user)}
                        className="text-blue-600 hover:text-blue-900"
                        title="Editar"
                      >
                        <Edit className="h-5 w-5" />
                      </button>
                      <button
                        onClick={() => handleDelete(user)}
                        className="text-red-600 hover:text-red-900"
                        title="Eliminar"
                      >
                        <Trash2 className="h-5 w-5" />
                      </button>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Modal */}
      {showModal && (
        <div className="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50">
          <div className="relative top-20 mx-auto p-5 border w-96 shadow-lg rounded-md bg-white">
            <div className="mt-3">
              <h3 className="text-lg font-medium text-gray-900 mb-4">
                {editingUser ? 'Editar Empleado' : 'Nuevo Empleado'}
              </h3>

              {generatedPin && (
                <div className="mb-4 p-4 bg-green-50 border border-green-200 rounded-lg">
                  <p className="text-sm font-medium text-green-800 mb-2">
                    PIN Generado:
                  </p>
                  <div className="flex items-center gap-2">
                    <code className="text-2xl font-mono font-bold text-green-900 bg-white px-4 py-2 rounded border-2 border-green-300">
                      {generatedPin}
                    </code>
                    <button
                      onClick={copyPinToClipboard}
                      className="p-2 text-green-700 hover:bg-green-100 rounded"
                      title="Copiar PIN"
                    >
                      {copiedPin ? (
                        <Check className="h-5 w-5" />
                      ) : (
                        <Copy className="h-5 w-5" />
                      )}
                    </button>
                  </div>
                  <p className="text-xs text-green-700 mt-2">
                    Guarda este PIN. El empleado lo necesitará para iniciar sesión en la app.
                  </p>
                </div>
              )}

              {generatedPassword && (
                <div className="mb-4 p-4 bg-green-50 border border-green-200 rounded-lg">
                  <p className="text-sm font-medium text-green-800 mb-2">
                    Contraseña generada:
                  </p>
                  <div className="flex items-center gap-2">
                    <code className="text-xl font-mono font-bold text-green-900 bg-white px-4 py-2 rounded border-2 border-green-300">
                      {generatedPassword}
                    </code>
                    <button
                      onClick={copyPasswordToClipboard}
                      className="p-2 text-green-700 hover:bg-green-100 rounded"
                      title="Copiar contraseña"
                    >
                      {copiedPassword ? <Check className="h-5 w-5" /> : <Copy className="h-5 w-5" />}
                    </button>
                  </div>
                  <p className="text-xs text-green-700 mt-2">
                    Guarda esta contraseña. El administrador la necesitará para iniciar sesión en el dashboard web.
                  </p>
                </div>
              )}

              <form onSubmit={handleSubmit} className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700">
                    Nombre
                  </label>
                  <input
                    type="text"
                    required
                    value={formData.name}
                    onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                    className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-primary-500 focus:ring-primary-500"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700">
                    Email
                  </label>
                  <input
                    type="email"
                    required
                    value={formData.email}
                    onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                    className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-primary-500 focus:ring-primary-500"
                    disabled={!!editingUser}
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700">
                    Rol
                  </label>
                  <select
                    value={formData.role}
                    onChange={(e) => setFormData({ ...formData, role: e.target.value as 'admin' | 'captador' })}
                    className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-primary-500 focus:ring-primary-500"
                  >
                    <option value="captador">Captador</option>
                    <option value="admin">Administrador</option>
                  </select>
                </div>

                <div className="flex items-center">
                  <input
                    type="checkbox"
                    id="is_active"
                    checked={formData.is_active}
                    onChange={(e) => setFormData({ ...formData, is_active: e.target.checked })}
                    className="h-4 w-4 text-primary-600 focus:ring-primary-500 border-gray-300 rounded"
                  />
                  <label htmlFor="is_active" className="ml-2 block text-sm text-gray-900">
                    Activo
                  </label>
                </div>

                {error && (
                  <div className="text-red-600 text-sm">{error}</div>
                )}

                <div className="flex gap-2 justify-end">
                  <button
                    type="button"
                    onClick={() => {
                      setShowModal(false);
                      setGeneratedPin(null);
                      setGeneratedPassword(null);
                    }}
                    className="btn btn-secondary"
                  >
                    {(generatedPin || generatedPassword) ? 'Cerrar' : 'Cancelar'}
                  </button>
                  {!generatedPin && !generatedPassword && (
                    <button type="submit" className="btn btn-primary">
                      {editingUser ? 'Actualizar' : 'Crear'}
                    </button>
                  )}
                </div>
              </form>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

